'use client';

import React, { useEffect, useRef, useState } from 'react';
import dynamic from 'next/dynamic';
import { motion, AnimatePresence, useScroll, useSpring, useTransform } from 'framer-motion';
import {
  Download, Github, ChevronDown, Music2, ShieldCheck, Zap, Library,
  Smartphone, Mail, Globe, ExternalLink, Cpu, Layers, Play,
  Star, Users, Headset, HardDrive, Share2, Search, Heart,
  Settings, Radio, Headphones, Disc
} from 'lucide-react';
import Lenis from 'lenis';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

const HeroVisual = dynamic(() => import('./components/HeroVisual'), {
  ssr: false,
  loading: () => (
    <div className="absolute inset-0 bg-[#020202] z-0 flex items-center justify-center">
       <div className="absolute inset-0 bg-gradient-to-br from-[#6C5CE7]/5 to-transparent opacity-30" />
       <div className="w-20 h-[1px] bg-white/20 animate-pulse" />
    </div>
  )
});

function CustomCursor() {
  const cursorRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const moveCursor = (e: MouseEvent) => {
      if (cursorRef.current) {
        gsap.to(cursorRef.current, { x: e.clientX, y: e.clientY, duration: 0.5, ease: 'power3.out' });
      }
    };
    window.addEventListener('mousemove', moveCursor);
    return () => window.removeEventListener('mousemove', moveCursor);
  }, []);
  return <div ref={cursorRef} className="fixed top-0 left-0 w-8 h-8 bg-white/20 border border-white/40 rounded-full pointer-events-none z-[999] -translate-x-1/2 -translate-y-1/2 backdrop-blur-sm hidden md:block" />;
}

export default function MelodifyOfficial() {
  const [activeIndex, setActiveIndex] = useState(0);
  const containerRef = useRef(null);
  const [settings, setSettings] = useState({
    apkLink: 'https://github.com/anshkesharwani0807-rgb/Melodify_Official/releases/download/v2.0.0-dev5/Melodify-v2.0.2-dev5-.Final.apk',
    version: 'v2.0.0-dev5',
    bio: '"I believe software should be as beautiful as the art it carries. Melodify is my commitment to a premium, privacy-focused future for Android music."',
    github: 'https://github.com/anshkesharwani0807-rgb/Melodify_Official/releases/tag/v2.0.0-dev5',
    creatorPic: '',
    email: 'anshkesharwani0807@gmail.com',
    appLogo: '/logo.png',
    screenshots: [
      '/screenshots/Splash_Screen.jpeg',
      '/screenshots/Home_Screen.jpeg',
      '/screenshots/Player_Screen.jpeg',
      '/screenshots/Search_screen.jpeg',
      '/screenshots/LIbrary_Screen.jpeg',
      '/screenshots/Setting_Screen.jpeg'
    ]
  });

  const SCREENSHOTS_TITLES = ['Sonic Gateway', 'Pure Discovery', 'Immersive Flow', 'Zero Latency', 'Digital Archive', 'Premium Control'];
  const SCREENSHOTS_DESCS = [
    'A minimalist entry to your personal soundscape.',
    'Intelligent curation meets your daily mood.',
    'Design that breathes with every note you play.',
    'Find your rhythm in a heartbeat.',
    'Your music, organized with military precision.',
    'Fine-tune your experience with surgical precision.'
  ];

  const SCREENSHOTS_CONFIG = settings.screenshots.map((src, i) => ({
    id: `screen-${i}`,
    title: SCREENSHOTS_TITLES[i] || 'Interface Preview',
    src: src,
    desc: SCREENSHOTS_DESCS[i] || 'Experience the premium flow of Melodify.'
  }));

  useEffect(() => {
    fetchSettings();
  }, []);

  useEffect(() => {
    const lenis = new Lenis({ duration: 1.2, easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)) });
    function raf(time: number) { lenis.raf(time); requestAnimationFrame(raf); }
    requestAnimationFrame(raf);

    const ctx = gsap.context(() => {
      gsap.from(".reveal-text", { y: 100, opacity: 0, duration: 1.5, stagger: 0.2, ease: "power4.out", scrollTrigger: { trigger: ".reveal-text", start: "top 85%" } });
      ScrollTrigger.create({
        trigger: "#showcase", start: "top top", end: "+=400", pin: "#phone-anchor", scrub: 1,
        onUpdate: (self) => setActiveIndex(Math.min(Math.floor(self.progress * SCREENSHOTS_CONFIG.length), SCREENSHOTS_CONFIG.length - 1))
      });
    });
    return () => { ctx.revert(); lenis.destroy(); };
  }, [SCREENSHOTS_CONFIG.length]);

  const fetchSettings = async () => {
    try {
      const res = await fetch('/api/settings');
      if (res.ok) {
        const data = await res.json();
        setSettings(prev => ({
          ...prev,
          ...data,
          screenshots: Array.isArray(data.screenshots) ? data.screenshots : prev.screenshots
        }));
      }
    } catch (err) {
      console.error('Failed to load settings');
    }
  };

  return (
    <main ref={containerRef} className="bg-[#020202] text-white relative selection:bg-[#6C5CE7]/40">
      <CustomCursor />

      <div className="fixed inset-0 pointer-events-none z-0 overflow-hidden">
        <div className="absolute top-[-10%] left-[-10%] w-[60%] h-[60%] bg-[#6C5CE7]/10 rounded-full blur-[140px] animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-[#00E5FF]/10 rounded-full blur-[140px] animate-pulse" />
        <div className="absolute top-[30%] left-[40%] w-px h-full bg-gradient-to-b from-transparent via-white/10 to-transparent opacity-20" />
      </div>

      <HeroVisual />

      {/* 🧭 NAVIGATION */}
      <nav className="fixed w-full z-[100] px-6 md:px-12 py-8 flex justify-between items-center backdrop-blur-md border-b border-white/[0.03]">
        <div className="flex items-center gap-4 group cursor-pointer">
          <div className="w-12 h-12 bg-white rounded-2xl flex items-center justify-center group-hover:bg-[#6C5CE7] transition-all duration-700 shadow-glow overflow-hidden">
            <img src={settings.appLogo || '/logo.png'} className="w-full h-full object-cover" />
          </div>
          <div className="flex flex-col leading-none">
            <span className="font-black text-2xl tracking-tighter uppercase italic">Melodify</span>
            <span className="text-[8px] font-bold tracking-[0.5em] text-white/60 uppercase">Offline Lab</span>
          </div>
        </div>
        <div className="hidden lg:flex gap-12 text-[10px] font-black tracking-[0.4em] text-white/60 uppercase items-center">
          <a href="#showcase" className="hover:text-white transition-all">Showcase</a>
          <a href="#features" className="hover:text-white transition-all">Features</a>
          <a href="#stats" className="hover:text-white transition-all">Insights</a>
          <a href="#developer" className="hover:text-white transition-all">Creator</a>
          <a href="/admin" className="px-6 py-2 rounded-full border border-[#6C5CE7]/30 glass text-[#6C5CE7] hover:bg-[#6C5CE7] hover:text-white transition-all">Portal</a>
          <span className="px-4 py-1.5 rounded-full border border-white/5 bg-white/[0.02] text-white/60">{settings.version}</span>
        </div>
      </nav>

      {/* ⚡ HERO SECTION */}
      <section className="relative min-h-[120vh] flex flex-col items-center justify-center pt-20 px-6 z-10 overflow-hidden">
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 1.5, ease: [0.16, 1, 0.3, 1] }} className="text-center relative">
          <div className="mb-10 inline-flex items-center gap-3 px-5 py-2 rounded-full glass border-white/10">
            <span className="w-2 h-2 rounded-full bg-[#6C5CE7] animate-ping" />
            <span className="text-[10px] font-black tracking-[0.4em] text-white/70 uppercase">Engineered for Android High-Fidelity</span>
          </div>
          <h1 className="text-[14vw] md:text-[13rem] font-black leading-[0.8] tracking-tighter mb-10 mix-blend-difference">
            LISTEN. <br/> <span className="italic text-transparent bg-clip-text bg-gradient-to-r from-[#6C5CE7] via-white to-[#00E5FF] opacity-90 text-white">UNBOUND.</span>
          </h1>
          <p className="text-white/60 text-lg md:text-2xl max-w-4xl mx-auto mb-16 font-medium leading-relaxed tracking-tight">
            An uncompromising offline music sanctuary. Beautifully minimalist. <br/>
            No analytics. No telemetry. Just you and your collection.
          </p>
          <div className="flex flex-col sm:flex-row gap-8 justify-center items-center">
            <a href={settings.apkLink} target="_blank" className="px-14 py-7 rounded-[2rem] bg-white text-black font-black text-xl flex items-center gap-4 shadow-2xl hover:scale-105 transition-transform active:scale-95">
              <Download size={28} strokeWidth={3} /> GET MELODIFY
            </a>
            <a href={settings.github} target="_blank" className="px-14 py-7 rounded-[2rem] glass text-white font-black text-xl flex items-center gap-4 hover:bg-white/5 border-white/10 transition-all">
              <Github size={28} /> SOURCE
            </a>
          </div>
        </motion.div>
      </section>

      {/* 🚀 QUICK STATS */}
      <section id="stats" className="pt-40 pb-10 relative z-10 px-6">
        <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-12 text-center">
           {[
             { label: "Indexing Speed", val: "10k+", unit: "Tracks/sec", icon: <Cpu className="text-[#6C5CE7]" /> },
             { label: "Privacy Rating", val: "100%", unit: "Offline", icon: <ShieldCheck className="text-emerald-400" /> },
             { label: "App Latency", val: "0.2", unit: "ms", icon: <Zap className="text-[#00E5FF]" /> },
             { label: "File Support", val: "24", unit: "Bit Lossless", icon: <Headphones className="text-pink-500" /> },
           ].map((s, i) => (
             <div key={i} className="glass p-10 rounded-[3rem] border-white/5 flex flex-col items-center gap-6 group hover:border-white/20 transition-all">
                <div className="w-14 h-14 rounded-2xl bg-white/5 flex items-center justify-center group-hover:scale-110 transition-transform">{s.icon}</div>
                <div>
                   <h4 className="text-[10px] font-black tracking-widest text-white/30 uppercase mb-2">{s.label}</h4>
                   <div className="flex items-baseline gap-2">
                      <span className="text-5xl font-black">{s.val}</span>
                      <span className="text-xs font-bold text-white/40">{s.unit}</span>
                   </div>
                </div>
             </div>
           ))}
        </div>
      </section>

      {/* 📱 SHOWCASE */}
      <section id="showcase" className="relative z-10 bg-[#050505]">
        <div id="phone-anchor" className="h-screen flex items-center justify-center sticky top-0 overflow-hidden bg-black/40 backdrop-blur-sm">
          <div className="max-w-[1600px] w-full grid grid-cols-1 lg:grid-cols-2 gap-20 items-center px-12">
            <div className="hidden lg:block">
              <AnimatePresence mode="wait">
                <motion.div key={SCREENSHOTS_CONFIG[activeIndex]?.id} initial={{ opacity: 0, x: -100 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 100 }} transition={{ duration: 0.8 }} className="space-y-12">
                  <span className="text-[#6C5CE7] font-black text-sm uppercase tracking-[0.8em]">Module 0{activeIndex + 1}</span>
                  <h2 className="text-[8rem] font-black tracking-tighter leading-none">{SCREENSHOTS_CONFIG[activeIndex]?.title}</h2>
                  <p className="text-white/40 text-3xl leading-snug max-w-xl font-medium">{SCREENSHOTS_CONFIG[activeIndex]?.desc}</p>
                  <div className="flex gap-4">
                    {SCREENSHOTS_CONFIG.map((_, i) => (
                      <div key={i} className={`h-1 rounded-full transition-all duration-700 ${i === activeIndex ? 'w-24 bg-white' : 'w-8 bg-white/10'}`} />
                    ))}
                  </div>
                </motion.div>
              </AnimatePresence>
            </div>
            <div className="flex justify-center perspective-[3000px]">
              <div className="w-[320px] md:w-[380px] aspect-[9/19.5] bg-[#050505] rounded-[4rem] border-[12px] border-[#151515] relative overflow-hidden shadow-2xl ring-1 ring-white/10">
                <AnimatePresence mode="wait">
                  <motion.img key={SCREENSHOTS_CONFIG[activeIndex]?.id} src={SCREENSHOTS_CONFIG[activeIndex]?.src} initial={{ opacity: 0, scale: 1.1 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.6 }} className="w-full h-full object-cover" />
                </AnimatePresence>
              </div>
            </div>
          </div>
        </div>
        <div className="h-[20vh]" />
      </section>

      {/* 💎 FEATURES */}
      <section id="features" className="pt-0 pb-60 px-6 max-w-[1400px] mx-auto z-10 relative">
        <div className="mb-32">
           <h2 className="text-7xl md:text-[10rem] font-black tracking-tighter leading-none mb-8">CORE <br/> <span className="text-white/20 italic">ARCHIVE.</span></h2>
           <p className="text-white/40 text-2xl font-medium max-w-xl italic">Premium by design, private by choice.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
           <div className="md:col-span-8 glass p-16 md:p-24 rounded-[4rem] group hover:border-[#6C5CE7]/30 transition-all border-white/5">
              <ShieldCheck size={50} className="text-[#00E5FF] mb-12" />
              <h3 className="text-6xl font-black mb-8 tracking-tighter uppercase leading-[0.85]">ZERO TRACKING. <br/> TOTAL CONTROL.</h3>
              <p className="text-white/40 text-2xl leading-relaxed max-w-2xl font-medium">Your musical identity is yours alone. We use native file system access to scan your local storage with zero cloud telemetry. Built for the privacy-first generation.</p>
           </div>
           <div className="md:col-span-4 flex flex-col gap-8 text-center">
              <div className="flex-1 glass p-12 rounded-[3.5rem] border-white/5 group flex flex-col items-center justify-center">
                 <Zap size={40} className="text-[#6C5CE7] mb-6 group-hover:rotate-12 transition-transform" />
                 <h3 className="text-3xl font-black mb-2 tracking-tight">120Hz Flow</h3>
                 <p className="text-white/30 text-base leading-relaxed">Silky smooth animations optimized for high-refresh rate displays.</p>
              </div>
              <div className="flex-1 glass p-12 rounded-[3.5rem] border-white/5 group flex flex-col items-center justify-center">
                 <Headset size={40} className="text-pink-500 mb-6 group-hover:scale-110 transition-transform" />
                 <h3 className="text-3xl font-black mb-2 tracking-tight">Lossless HQ</h3>
                 <p className="text-white/30 text-base leading-relaxed">Native support for studio-quality audio formats like FLAC and ALAC.</p>
              </div>
           </div>
        </div>
      </section>

      {/* 📦 THE FINAL CONVERSION */}
      <section className="py-80 text-center px-6 relative overflow-hidden bg-[#020202]">
        <div className="absolute inset-0 z-0 flex items-center justify-center opacity-[0.02]">
           <h2 className="text-[35vw] font-black tracking-tighter select-none italic uppercase">AURORA</h2>
        </div>

        <div className="relative z-10 max-w-4xl mx-auto">
           <span className="text-[#6C5CE7] font-black tracking-[0.8em] text-xs uppercase mb-10 block">End of the line.</span>
           <h3 className="text-7xl md:text-[10rem] font-black tracking-tighter leading-none mb-20 uppercase">HEAR THE <br/> DIFFERENCE.</h3>

           <a
             href={settings.apkLink}
             target="_blank"
             className="inline-block px-24 py-10 rounded-[3rem] bg-[#6C5CE7] text-white font-black text-3xl shadow-[0_40px_80px_rgba(108,92,231,0.3)] hover:shadow-[0_50px_100px_rgba(108,92,231,0.5)] transition-all cursor-pointer"
           >
             DOWNLOAD {settings.version}
           </a>
        </div>
      </section>

      {/* 👤 THE CREATOR */}
      <section id="developer" className="py-60 px-6 relative">
         <div className="max-w-5xl mx-auto glass p-20 md:p-32 rounded-[5rem] text-center relative z-10 shadow-glow border-white/5">
            <motion.div whileHover={{ scale: 1.1, rotate: 5 }} className="w-28 h-28 bg-white rounded-[2rem] flex items-center justify-center mx-auto mb-16 shadow-2xl cursor-pointer overflow-hidden">
               {settings.creatorPic ? (
                  <img src={settings.creatorPic} className="w-full h-full object-cover" />
               ) : (
                  <span className="text-5xl font-black text-black italic">AK</span>
               )}
            </motion.div>
            <h2 className="text-6xl md:text-[8rem] font-black tracking-tighter leading-none mb-6">Ansh Kesharwani</h2>
            <p className="text-[#6C5CE7] font-black tracking-[0.8em] text-xs uppercase mb-16">Independent Product Architect</p>
            <p className="text-white/70 text-3xl md:text-5xl leading-tight mb-20 font-medium tracking-tighter">
               {settings.bio}
            </p>
            <div className="flex flex-wrap justify-center gap-6">
               <a href={settings.github} target="_blank" className="px-14 py-6 rounded-2xl bg-white text-black font-black flex items-center gap-3 hover:scale-105 transition-all text-xl">
                 <Github size={24} /> GITHUB
               </a>
               <a
                 href={`https://mail.google.com/mail/?view=cm&fs=1&to=${settings.email || 'anshkesharwani0807@gmail.com'}`}
                 target="_blank"
                 rel="noopener noreferrer"
                 className="px-14 py-6 rounded-2xl glass text-white font-black flex items-center gap-3 hover:bg-white/10 transition-all text-xl cursor-pointer relative z-[100]"
               >
                 <Mail size={24} /> CONTACT
               </a>
            </div>
            <p className="mt-8 text-white/30 font-bold tracking-widest text-sm uppercase">
               Direct: {settings.email || 'anshkesharwani0807@gmail.com'}
            </p>
         </div>
      </section>

      {/* 🏁 FOOTER */}
      <footer className="py-32 px-12 border-t border-white/5 bg-[#010101] text-center">
        <div className="flex items-center justify-center gap-4 mb-16 opacity-60">
           <div className="w-8 h-8 rounded-lg overflow-hidden grayscale">
              <img src={settings.appLogo || '/logo.png'} className="w-full h-full object-cover" />
           </div>
           <span className="font-black text-3xl tracking-tighter italic uppercase">MELODIFY</span>
        </div>
        <div className="flex justify-center gap-16 text-[10px] font-black tracking-[0.4em] text-white/70 uppercase mb-16">
           <a href="#" className="hover:text-white transition-colors">Privacy</a>
           <a href="/admin" className="hover:text-white transition-colors text-[#6C5CE7]">Developer Portal</a>
           <a href={settings.github} target="_blank" className="hover:text-white transition-colors">Source</a>
        </div>
        <p className="text-white/50 font-black tracking-[0.4em] uppercase text-[10px]">
          &copy; 2026 OFFICIAL MELODIFY WEB EXPERIENCE &bull; DESIGNED FOR THE FUTURE
        </p>
      </footer>
    </main>
  );
}
