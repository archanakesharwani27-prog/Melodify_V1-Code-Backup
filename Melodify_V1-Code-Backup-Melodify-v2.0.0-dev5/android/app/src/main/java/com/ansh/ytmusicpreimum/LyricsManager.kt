package com.ansh.ytmusicpreimum

import android.util.LruCache
import com.ansh.ytmusicpreimum.data.LyricsEntity
import com.ansh.ytmusicpreimum.data.TrackDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LyricLine(val time: Long, val text: String)

data class LyricsState(
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val isSynced: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

object LyricsManager {
    private val memoryCache = LruCache<String, LyricsState>(30)
    private var trackDao: TrackDao? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val RECORD_LABELS = listOf(
        "t-series", "tseries", "sony music", "sony", "tips official", "tips", 
        "zee music", "zeemusic", "speed records", "universal music", "saregama", 
        "yrf", "white hill", "geet mp3", "desi music factory", "dm - desi music factory",
        "aditya music", "lahari music", "svf", "vst music", "vevo", "records", "music"
    )

    private val POPULAR_SINGERS = listOf(
        "Arijit Singh", "Yo Yo Honey Singh", "Honey Singh", "Atif Aslam", "Shreya Ghoshal",
        "Neha Kakkar", "Jubin Nautiyal", "B Praak", "Diljit Dosanjh", "Badshah",
        "Sidhu Moose Wala", "Karan Aujla", "AP Dhillon", "Sunidhi Chauhan", "Sonu Nigam",
        "Mohit Chauhan", "KK", "Kumar Sanu", "Udit Narayan", "Alka Yagnik", "Lata Mangeshkar",
        "Kishore Kumar", "Pritam", "A.R. Rahman", "Anirudh", "Darshan Raval", "Armaan Malik",
        "Guru Randhawa", "Hardy Sandhu", "Vishal Mishra", "Jasleen Royal", "Paradox", "Stebin Ben"
    )

    fun init(dao: TrackDao) {
        this.trackDao = dao
    }

    suspend fun getLyrics(videoId: String, title: String, artist: String, durationSeconds: Int = 0): LyricsState = withContext(Dispatchers.IO) {
        // 1. Memory Cache
        memoryCache.get(videoId)?.let { return@withContext it }

        // 2. Disk Cache
        try {
            trackDao?.getLyrics(videoId)?.let { entity ->
                if (entity.expiryTime > System.currentTimeMillis()) {
                    val script = detectScript(entity.lyrics)
                    if (script != LyricsScript.REGIONAL_OTHER) {
                        val state = parseLyrics(entity.lyrics, entity.isSynced)
                        // If duration is known and lyrics are synced, check for duration mismatch
                        val durationMismatch = if (durationSeconds > 0 && state.isSynced && state.lines.isNotEmpty()) {
                            val lastLineSec = state.lines.last().time / 1000.0
                            lastLineSec > (durationSeconds + 5.0) || (durationSeconds > 45 && lastLineSec < (durationSeconds - 35.0))
                        } else {
                            false
                        }

                        val hasForeignMarkers = entity.lyrics.contains(Regex("(?i)\\b(você|não|comigo|dança|porra|baile)\\b"))
                        if (!durationMismatch && !hasForeignMarkers) {
                            memoryCache.put(videoId, state)
                            return@withContext state
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricsManager", "Disk cache read error", e)
        }

        // 3. Network Fetch with Multi-Strategy Query Generation
        try {
            val isRemix = title.contains(Regex("(?i)(remix|\\bdj\\b|mashup|club\\s*mix|dance\\s*mix|slowed|reverb)"))
            val candidates = generateQueryCandidates(title, artist)
            android.util.Log.d("LyricsManager", "Query candidates for '$title' (remix=$isRemix): $candidates, dur=$durationSeconds")

            var bestLyrics: Pair<String, Boolean>? = null
            var bestScore = 0

            for ((queryArtist, queryTrack, querySearch) in candidates) {
                // A. Direct GET if artist & track are both distinct
                if (queryArtist.isNotBlank() && queryTrack.isNotBlank()) {
                    try {
                        val durParam = if (durationSeconds > 0) "&duration=$durationSeconds" else ""
                        val url = "https://lrclib.net/api/get?artist=${URLEncoder.encode(queryArtist, "UTF-8")}&track=${URLEncoder.encode(queryTrack, "UTF-8")}$durParam"
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Melodify Music App v2.0")
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                if (body.isNotBlank()) {
                                    val json = JSONObject(body)
                                    val candidateTrack = json.optString("trackName", "")
                                    val candidateArtist = json.optString("artistName", "")
                                    val candidateDur = json.optDouble("duration", 0.0)
                                    val synced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                                    val plain = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                                    
                                    val candidateText = synced ?: plain
                                    if (candidateText != null) {
                                        val canBeSynced = synced != null && (!isRemix || (durationSeconds > 0 && kotlin.math.abs(candidateDur - durationSeconds) <= 4.0))
                                        val textToUse = if (!canBeSynced && synced != null) {
                                            synced.replace(Regex("\\[\\d+:\\d+\\.?\\d*\\]"), "").trim()
                                        } else {
                                            candidateText
                                        }
                                        
                                        val score = scoreCandidate(
                                            text = textToUse,
                                            isSynced = canBeSynced,
                                            candidateDuration = candidateDur,
                                            targetDurationSeconds = durationSeconds,
                                            queryTrack = queryTrack,
                                            candidateTrack = candidateTrack,
                                            queryArtist = queryArtist.ifBlank { artist },
                                            candidateArtist = candidateArtist,
                                            isRemix = isRemix
                                        )
                                        if (score > bestScore) {
                                            bestLyrics = Pair(textToUse, canBeSynced)
                                            bestScore = score
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("LyricsManager", "Direct GET failed for $queryTrack: ${e.message}")
                    }
                }

                if (bestScore >= 160) break // Perfect match found

                // B. LRCLIB Search API
                if (querySearch.isNotBlank()) {
                    try {
                        val searchUrl = "https://lrclib.net/api/search?q=${URLEncoder.encode(querySearch, "UTF-8")}"
                        val searchRequest = Request.Builder()
                            .url(searchUrl)
                            .header("User-Agent", "Melodify Music App v2.0")
                            .build()
                        client.newCall(searchRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                if (body.isNotBlank()) {
                                    val jsonArray = org.json.JSONArray(body)
                                    for (i in 0 until jsonArray.length().coerceAtMost(16)) {
                                        val item = jsonArray.getJSONObject(i)
                                        val candidateTrack = item.optString("trackName", "")
                                        val candidateArtist = item.optString("artistName", "")
                                        val candidateDur = item.optDouble("duration", 0.0)
                                        val synced = item.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                                        val plain = item.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                                        
                                        val candidateText = synced ?: plain
                                        if (candidateText != null) {
                                            val canBeSynced = synced != null && (!isRemix || (durationSeconds > 0 && kotlin.math.abs(candidateDur - durationSeconds) <= 4.0))
                                            val textToUse = if (!canBeSynced && synced != null) {
                                                synced.replace(Regex("\\[\\d+:\\d+\\.?\\d*\\]"), "").trim()
                                            } else {
                                                candidateText
                                            }

                                            val score = scoreCandidate(
                                                text = textToUse,
                                                isSynced = canBeSynced,
                                                candidateDuration = candidateDur,
                                                targetDurationSeconds = durationSeconds,
                                                queryTrack = queryTrack,
                                                candidateTrack = candidateTrack,
                                                queryArtist = queryArtist.ifBlank { artist },
                                                candidateArtist = candidateArtist,
                                                isRemix = isRemix
                                            )
                                            if (score > bestScore) {
                                                bestLyrics = Pair(textToUse, canBeSynced)
                                                bestScore = score
                                            }
                                            if (bestScore >= 160) break
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("LyricsManager", "Search failed for $querySearch: ${e.message}")
                    }
                }

                if (bestScore >= 160) break
            }

            val selectedLyrics = bestLyrics
            if (selectedLyrics != null && bestScore > 40) {
                val (lyricsToStore, isSynced) = selectedLyrics
                val expiry = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
                try {
                    trackDao?.insertLyrics(LyricsEntity(videoId, lyricsToStore, isSynced, expiry))
                } catch (e: Exception) {
                    android.util.Log.e("LyricsManager", "Database cache insert error", e)
                }

                val state = parseLyrics(lyricsToStore, isSynced)
                memoryCache.put(videoId, state)
                return@withContext state
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricsManager", "Error fetching lyrics", e)
        }

        return@withContext LyricsState(error = "Lyrics not available")
    }

    private data class QueryCandidate(val artist: String, val track: String, val searchQuery: String)

    enum class LyricsScript {
        LATIN,          // English / Romanized Hinglish (Preferred for 99% of Bollywood/pop users)
        DEVANAGARI,     // Hindi / Sanskrit (\u0900..\u097F)
        REGIONAL_OTHER  // Gurmukhi (\u0A00..\u0A7F), Arabic/Urdu (\u0600..\u08FF), etc.
    }

    private fun detectScript(text: String): LyricsScript {
        var latinCount = 0
        var devanagariCount = 0
        var regionalCount = 0

        for (ch in text) {
            when {
                (ch in 'a'..'z') || (ch in 'A'..'Z') -> latinCount++
                ch in '\u0900'..'\u097F' -> devanagariCount++
                ch in '\u0600'..'\u08FF' || ch in '\u0A00'..'\u0A7F' || ch in '\u0980'..'\u09FF' -> regionalCount++
            }
        }

        return when {
            latinCount >= devanagariCount && latinCount >= regionalCount && latinCount > 15 -> LyricsScript.LATIN
            devanagariCount >= regionalCount && devanagariCount > 15 -> LyricsScript.DEVANAGARI
            else -> LyricsScript.REGIONAL_OTHER
        }
    }

    private fun scoreCandidate(
        text: String,
        isSynced: Boolean,
        candidateDuration: Double,
        targetDurationSeconds: Int,
        queryTrack: String,
        candidateTrack: String,
        queryArtist: String,
        candidateArtist: String,
        isRemix: Boolean
    ): Int {
        // 1. Strict Title Validation: Disqualify candidate if titles do not meaningfully match
        if (!hasTitleMatch(queryTrack, candidateTrack, queryArtist, candidateArtist)) {
            return -1000
        }

        // 2. Reject foreign scripts (e.g. Urdu, Arabic, Gurmukhi if Latin/Devanagari was requested)
        val script = detectScript(text)
        if (script == LyricsScript.REGIONAL_OTHER) {
            return -500
        }

        var score = when (script) {
            LyricsScript.LATIN -> if (isSynced) 100 else 60
            LyricsScript.DEVANAGARI -> if (isSynced) 75 else 45
            LyricsScript.REGIONAL_OTHER -> 0
        }

        // 3. Artist Matching Bonus
        if (queryArtist.isNotBlank() && candidateArtist.isNotBlank()) {
            val qA = queryArtist.lowercase().trim()
            val cA = candidateArtist.lowercase().trim()
            if (qA in cA || cA in qA) {
                score += 30
            }
        }

        // 4. Duration Proximity Scoring
        if (targetDurationSeconds > 0 && candidateDuration > 0) {
            val diff = kotlin.math.abs(candidateDuration - targetDurationSeconds)
            when {
                diff <= 2.0 -> score += 60 // Spot on match
                diff <= 5.0 -> score += 35
                diff <= 10.0 -> score += 10
                diff <= 20.0 -> score -= 25
                else -> {
                    // Severe duration difference (>20s)
                    if (isRemix) {
                        // For remixes, we tolerate duration differences ONLY as plain lyrics
                        score -= 20
                    } else {
                        score -= 80 // Wrong version/track
                    }
                }
            }
        }

        return score
    }

    private fun hasTitleMatch(
        queryTrack: String,
        candidateTrack: String,
        queryArtist: String = "",
        candidateArtist: String = ""
    ): Boolean {
        val qClean = cleanForMatch(queryTrack)
        val cClean = cleanForMatch(candidateTrack)

        if (qClean.isBlank() || cClean.isBlank()) return false

        // Exact match or direct containment
        if (qClean == cClean || cClean.contains(qClean) || qClean.contains(cClean)) {
            // Guard against numeric-only matches (e.g. "2026") matching unrelated foreign tracks
            val isNumericOnly = qClean.all { it.isDigit() || it.isWhitespace() }
            if (isNumericOnly) {
                // For purely numeric titles like "2026", candidate MUST match artist or identified singer
                val qArtistClean = cleanForMatch(queryArtist)
                val cArtistClean = cleanForMatch(candidateArtist)
                if (qArtistClean.isNotBlank() && cArtistClean.isNotBlank()) {
                    val artistWords = qArtistClean.split(" ").filter { it.length >= 2 }
                    val hasArtistOverlap = artistWords.any { cArtistClean.contains(it) }
                    if (!hasArtistOverlap) return false
                } else if (cArtistClean.isNotBlank()) {
                    // If we don't have query artist, verify candidate isn't obscure foreign artist
                    val isPopular = POPULAR_SINGERS.any { candidateArtist.contains(it, ignoreCase = true) }
                    if (!isPopular) return false
                }
            }
            return true
        }

        // Word overlap checking
        val qWords = qClean.split(" ").filter { it.length >= 2 && it !in STOP_WORDS }
        val cWords = cClean.split(" ").filter { it.length >= 2 && it !in STOP_WORDS }

        if (qWords.isEmpty() || cWords.isEmpty()) return false

        val matchingWords = qWords.count { qw ->
            cWords.any { cw -> cw == qw || (cw.length >= 4 && qw.length >= 4 && (cw.startsWith(qw) || qw.startsWith(cw))) }
        }

        return if (qWords.size <= 2) {
            matchingWords == qWords.size
        } else {
            (matchingWords.toFloat() / qWords.size.toFloat()) >= 0.5f || matchingWords >= 2
        }
    }

    private val STOP_WORDS = setOf(
        "song", "songs", "track", "audio", "video", "official", "music", "full", "hd", "4k", "lyric", "lyrics"
    )

    private fun cleanForMatch(text: String): String {
        return text.lowercase()
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun transliterateDevanagari(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when (ch) {
                'अ' -> sb.append("a")
                'आ' -> sb.append("aa")
                'इ' -> sb.append("i")
                'ई' -> sb.append("ee")
                'उ' -> sb.append("u")
                'ऊ' -> sb.append("oo")
                'ए' -> sb.append("e")
                'ऐ' -> sb.append("ai")
                'ओ' -> sb.append("o")
                'औ' -> sb.append("au")
                'क' -> sb.append("k")
                'ख' -> sb.append("kh")
                'ग' -> sb.append("g")
                'घ' -> sb.append("gh")
                'च' -> sb.append("ch")
                'छ' -> sb.append("chh")
                'ज' -> sb.append("j")
                'झ' -> sb.append("jh")
                'ट' -> sb.append("t")
                'ठ' -> sb.append("th")
                'ड' -> sb.append("d")
                'ढ' -> sb.append("dh")
                'ण' -> sb.append("n")
                'त' -> sb.append("t")
                'थ' -> sb.append("th")
                'द' -> sb.append("d")
                'ध' -> sb.append("dh")
                'न' -> sb.append("n")
                'प' -> sb.append("p")
                'फ' -> sb.append("ph")
                'ब' -> sb.append("b")
                'भ' -> sb.append("bh")
                'म' -> sb.append("m")
                'य' -> sb.append("y")
                'र' -> sb.append("r")
                'ल' -> sb.append("l")
                'व' -> sb.append("v")
                'श' -> sb.append("sh")
                'ष' -> sb.append("sh")
                'स' -> sb.append("s")
                'ह' -> sb.append("h")
                'ा' -> sb.append("a")
                'ि' -> sb.append("i")
                'ी' -> sb.append("ee")
                'ु' -> sb.append("u")
                'ू' -> sb.append("oo")
                'े' -> sb.append("e")
                'ै' -> sb.append("ai")
                'ो' -> sb.append("o")
                'ौ' -> sb.append("au")
                'ं' -> sb.append("n")
                '्' -> { /* virama */ }
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    private fun generateQueryCandidates(rawTitle: String, rawArtist: String): List<QueryCandidate> {
        val candidates = mutableListOf<QueryCandidate>()

        val isLabel = RECORD_LABELS.any { rawArtist.lowercase().contains(it) }
        val cleanUploaderArtist = cleanArtistQuery(rawArtist)

        // Find if title contains a well-known singer
        val singerInTitle = POPULAR_SINGERS.find { rawTitle.contains(it, ignoreCase = true) }

        // Split title on delimiters like '|', '•', '/', or ':'
        val segments = rawTitle.split("|", "•", "/").map { it.trim() }
        val primarySegment = segments.firstOrNull() ?: rawTitle

        var extractedTitle = primarySegment
        var extractedArtist = ""

        if (primarySegment.contains(":")) {
            val parts = primarySegment.split(":", limit = 2)
            extractedTitle = cleanSearchQuery(parts[0])
            extractedArtist = cleanArtistQuery(parts[1])
        } else if (primarySegment.contains("-")) {
            val parts = primarySegment.split("-", limit = 2)
            extractedTitle = cleanSearchQuery(parts[0])
            extractedArtist = cleanArtistQuery(parts[1])
        } else if (primarySegment.contains("–")) {
            val parts = primarySegment.split("–", limit = 2)
            extractedTitle = cleanSearchQuery(parts[0])
            extractedArtist = cleanArtistQuery(parts[1])
        } else {
            extractedTitle = cleanSearchQuery(primarySegment)
        }

        val effectiveArtist = when {
            singerInTitle != null -> singerInTitle
            extractedArtist.isNotBlank() && !RECORD_LABELS.any { extractedArtist.lowercase().contains(it) } -> extractedArtist
            !isLabel && cleanUploaderArtist.isNotBlank() -> cleanUploaderArtist
            else -> ""
        }

        val isRemix = rawTitle.contains(Regex("(?i)(remix|\\bdj\\b|mashup|club\\s*mix|dance\\s*mix)"))

        // Candidate 0 (if remix): Search specifically for remix cut
        if (isRemix && extractedTitle.isNotBlank()) {
            candidates.add(QueryCandidate(effectiveArtist, "$extractedTitle remix", "$extractedTitle remix $effectiveArtist"))
        }

        // Candidate 1: Clean Track + Identified Singer/Artist
        if (extractedTitle.isNotBlank() && effectiveArtist.isNotBlank()) {
            candidates.add(QueryCandidate(effectiveArtist, extractedTitle, "$extractedTitle $effectiveArtist"))
        }

        // Candidate 2: Clean Track Alone (LRCLIB search often finds Indian tracks with exact title)
        if (extractedTitle.isNotBlank()) {
            candidates.add(QueryCandidate("", extractedTitle, extractedTitle))
        }

        // Candidate 3: Transliterated Devanagari (e.g. "जनम जनम" -> "janam janam")
        if (extractedTitle.any { it in '\u0900'..'\u097F' }) {
            val romanized = transliterateDevanagari(extractedTitle).trim()
            if (romanized.isNotBlank()) {
                val artistForRoman = if (effectiveArtist.any { it in '\u0900'..'\u097F' }) transliterateDevanagari(effectiveArtist).trim() else effectiveArtist
                candidates.add(QueryCandidate(artistForRoman, romanized, if (artistForRoman.isNotBlank()) "$romanized $artistForRoman" else romanized))
            }
        }

        // Candidate 4: Core first words of title (e.g. "Janam Janam" or "Payal")
        val words = extractedTitle.split(" ").filter { it.isNotBlank() }
        if (words.size > 2) {
            val shortTitle = words.take(2).joinToString(" ")
            candidates.add(QueryCandidate(effectiveArtist, shortTitle, if (effectiveArtist.isNotBlank()) "$shortTitle $effectiveArtist" else shortTitle))
        }

        return candidates
    }

    private fun parseLyrics(content: String, isSynced: Boolean): LyricsState {
        if (!isSynced) {
            return LyricsState(plainLyrics = content, isSynced = false)
        }

        val lines = mutableListOf<LyricLine>()
        val timeRegex = Regex("\\[(\\d+):(\\d+\\.?\\d*)\\]")

        content.lines().forEach { line ->
            val timeMatches = timeRegex.findAll(line).toList()
            if (timeMatches.isNotEmpty()) {
                val text = line.substring(timeMatches.last().range.last + 1).trim()
                if (text.isNotEmpty()) {
                    timeMatches.forEach { match ->
                        val min = match.groupValues[1].toLong()
                        val sec = match.groupValues[2].toFloat()
                        val timeMs = (min * 60 * 1000 + (sec * 1000).toLong())
                        lines.add(LyricLine(timeMs, text))
                    }
                }
            }
        }

        lines.sortBy { it.time }

        return if (lines.isNotEmpty()) {
            LyricsState(lines = lines, isSynced = true)
        } else {
            LyricsState(plainLyrics = content, isSynced = false)
        }
    }

    private fun cleanSearchQuery(query: String): String {
        var cleaned = query
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?i)(official\\s*video|official\\s*audio|music\\s*video|lyric\\s*video|full\\s*song|video\\s*song|lyrics?|4k|hd|audio|video|teaser|trailer)"), "")
            .replace(Regex("[,:;!_#]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val words = cleaned.split(" ").filter { it.isNotBlank() }
        if (words.size > 1) {
            val withoutStop = cleaned
                .replace(Regex("(?i)\\b(song|songs|track)\\b"), "")
                .replace(Regex("\\b202[0-9]\\b"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (withoutStop.isNotBlank()) {
                cleaned = withoutStop
            }
        }
        return cleaned.ifBlank { query.trim() }
    }

    private fun cleanArtistQuery(artist: String): String {
        return artist
            .replace(" - Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("Records", "", ignoreCase = true)
            .replace("Music", "", ignoreCase = true)
            .replace(Regex("(?i)(label|company|channel)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
