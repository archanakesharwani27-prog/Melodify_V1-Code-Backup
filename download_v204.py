import urllib.request
import re

url = "https://www.amazon.com/photos/shared/nVLzDZv4RoKmTGHYocnViw.opYlZzYJGFh7hWvFZBqXiW"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
try:
    html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')
    print("HTML length:", len(html))
    
    thumbnails = list(set(re.findall(r'https://thumbnails-photos\.amazon\.com/v1/thumbnail/[^\s"\'<>]+', html)))
    print("Found thumbnails:", len(thumbnails))
    for i, t in enumerate(thumbnails):
        full_url = re.sub(r'viewBox=\d+%2C\d+', 'viewBox=1920%2C1920', t)
        print(f"Downloading thumb {i}: {full_url}")
        try:
            req2 = urllib.request.Request(full_url, headers={'User-Agent': 'Mozilla/5.0'})
            data = urllib.request.urlopen(req2).read()
            filename = f"amazon_rejection_v204_{i}.png"
            with open(filename, "wb") as f:
                f.write(data)
            print(f"Saved {filename}, size={len(data)}")
        except Exception as e:
            print("Error downloading thumb:", e)

    videos = list(set(re.findall(r'https://[^\s"\'<>]+\.mp4[^\s"\'<>]*', html)))
    print("Found videos:", len(videos))
    for i, v in enumerate(videos):
        print(f"Video {i}: {v}")
except Exception as e:
    print("Error:", e)
