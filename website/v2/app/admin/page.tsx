'use client';

import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { ShieldCheck, LogOut, Package, User, Smartphone, Save, Image as ImageIcon, Plus, Trash2, CheckCircle2, Upload, Lock } from 'lucide-react';

export default function AdminPortal() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [key, setKey] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [saveStatus, setSaveStatus] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [settings, setSettings] = useState({
    apkLink: '',
    version: '',
    bio: '',
    github: '',
    email: 'anshkesharwani0807@gmail.com',
    creatorPic: '',
    appLogo: '/logo.png',
    screenshots: [] as string[],
    adminKey: ''
  });

  const [uploadTarget, setUploadTarget] = useState<'screenshots' | 'creator' | 'logo'>('screenshots');

  const SCREEN_LABELS = [
    'Splash Screen (Logo)',
    'Home Screen (Discovery)',
    'Player Screen (Playback)',
    'Search Screen',
    'Library Screen',
    'Settings Screen'
  ];

  useEffect(() => {
    checkSession();
  }, []);

  const checkSession = async () => {
    try {
      const res = await fetch('/api/settings');
      if (res.ok) {
        setIsLoggedIn(true);
      }
    } catch (err) {
      console.error('Session check failed');
    }
  };

  useEffect(() => {
    if (isLoggedIn) {
      fetchSettings();
    }
  }, [isLoggedIn]);

  const fetchSettings = async () => {
    try {
      const res = await fetch('/api/settings');
      const data = await res.json();
      if (res.status === 401) {
        setIsLoggedIn(false);
        return;
      }
      setSettings({
        ...data,
        screenshots: Array.isArray(data.screenshots) ? data.screenshots : [],
        adminKey: '' // Don't pre-fill password for security
      });
    } catch (err) {
      console.error('Failed to load settings');
    }
  };

  const handleLogin = async () => {
    setIsLoading(true);
    try {
      const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key })
      });
      const data = await res.json();
      if (data.success) {
        setIsLoggedIn(true);
      } else {
        alert('Unauthorized Access');
      }
    } catch (err) {
      alert('Login error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await fetch('/api/logout', { method: 'POST' });
      setIsLoggedIn(false);
    } catch (err) {
      console.error('Logout failed');
    }
  };

  const handleSave = async () => {
    setIsLoading(true);
    setSaveStatus('Saving...');
    try {
      const res = await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(settings)
      });
      if (res.ok) {
        setSaveStatus('Published Successfully!');
        setTimeout(() => setSaveStatus(''), 3000);
      }
    } catch (err) {
      setSaveStatus('Error saving changes');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    setIsLoading(true);
    try {
      const res = await fetch('/api/upload', {
        method: 'POST',
        body: formData,
      });
      const data = await res.json();
      if (data.url) {
        if (uploadTarget === 'creator') {
          setSettings({ ...settings, creatorPic: data.url });
        } else if (uploadTarget === 'logo') {
          setSettings({ ...settings, appLogo: data.url });
        } else {
          setSettings({ ...settings, screenshots: [...settings.screenshots, data.url] });
        }
      } else {
        alert('Upload failed: ' + data.error);
      }
    } catch (err) {
      alert('Upload error');
    } finally {
      setIsLoading(false);
    }
  };

  const updateScreenshot = (index: number, val: string) => {
    const newScreens = [...settings.screenshots];
    newScreens[index] = val;
    setSettings({ ...settings, screenshots: newScreens });
  };

  const addScreenshot = () => {
    setSettings({ ...settings, screenshots: [...settings.screenshots, ''] });
  };

  const removeScreenshot = (index: number) => {
    const newScreens = settings.screenshots.filter((_, i) => i !== index);
    setSettings({ ...settings, screenshots: newScreens });
  };

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen bg-[#020202] flex items-center justify-center p-6 text-white font-sans">
        <div className="bg-white/[0.02] backdrop-blur-3xl border border-white/5 p-12 rounded-[3rem] w-full max-w-md text-center shadow-[0_0_100px_rgba(108,92,231,0.1)]">
          <div className="w-16 h-16 bg-[#6C5CE7] rounded-2xl flex items-center justify-center mx-auto mb-8 rotate-6 shadow-glow">
            <ShieldCheck className="text-white w-8 h-8" />
          </div>
          <h1 className="text-3xl font-black mb-10 tracking-tighter uppercase italic">Developer Portal</h1>
          <input
            type="password"
            placeholder="ADMIN KEY"
            className="w-full bg-white/5 border border-white/10 p-5 rounded-2xl mb-6 text-center text-lg tracking-[0.5em] focus:border-[#6C5CE7] outline-none transition-all placeholder:opacity-20"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
          />
          <button
            onClick={handleLogin}
            className="w-full bg-white text-black font-black py-5 rounded-2xl hover:bg-[#6C5CE7] hover:text-white transition-all duration-500 shadow-xl"
          >
            UNLOCK ACCESS
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#020202] text-white p-6 md:p-20 font-sans">
      <div className="max-w-5xl mx-auto">
        <div className="flex justify-between items-center mb-16">
          <div>
            <h2 className="text-5xl font-black tracking-tighter uppercase italic">Control <span className="text-[#6C5CE7]">Center.</span></h2>
            <p className="text-white/30 font-medium tracking-tight mt-2">Update app binary, assets, and metadata instantly.</p>
          </div>
          <button
            onClick={handleLogout}
            className="p-5 bg-white/5 rounded-2xl hover:bg-red-500/10 hover:text-red-400 transition-all border border-white/5"
          >
            <LogOut size={24} />
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Distribution */}
          <div className="bg-white/[0.02] border border-white/5 p-10 rounded-[3rem] space-y-8">
            <h3 className="text-2xl font-black mb-4 flex items-center gap-4 italic uppercase">
              <Package className="text-[#00E5FF]" /> Distribution
            </h3>
            <div className="space-y-6">
               <div className="flex items-center gap-6 p-4 bg-white/5 rounded-2xl border border-white/10">
                  <div className="w-16 h-16 rounded-xl overflow-hidden bg-black border border-white/10 relative group">
                     <img src={settings.appLogo || '/logo.png'} className="w-full h-full object-cover" />
                     <button
                        onClick={() => { setUploadTarget('logo'); fileInputRef.current?.click(); }}
                        className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
                     >
                        <Upload size={16} />
                     </button>
                  </div>
                  <div className="flex-1">
                     <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-2 block">App Logo URL</label>
                     <input
                        type="text"
                        value={settings.appLogo}
                        onChange={(e) => setSettings({...settings, appLogo: e.target.value})}
                        className="w-full bg-white/5 border border-white/10 p-3 rounded-xl focus:border-[#6C5CE7] outline-none text-xs"
                        placeholder="/logo.png"
                     />
                  </div>
               </div>
              <div>
                <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-3 block ml-1">APK Release URL</label>
                <input
                  type="text"
                  value={settings.apkLink}
                  onChange={(e) => setSettings({...settings, apkLink: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 p-5 rounded-2xl focus:border-[#6C5CE7] outline-none transition-all font-medium"
                />
              </div>
              <div>
                <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-3 block ml-1">Version Number</label>
                <input
                  type="text"
                  value={settings.version}
                  onChange={(e) => setSettings({...settings, version: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 p-5 rounded-2xl focus:border-[#6C5CE7] outline-none transition-all font-medium"
                />
              </div>
            </div>
          </div>

          {/* Profile */}
          <div className="bg-white/[0.02] border border-white/5 p-10 rounded-[3rem] space-y-8">
            <h3 className="text-2xl font-black mb-4 flex items-center gap-4 italic uppercase">
              <User className="text-[#6C5CE7]" /> Creator Identity
            </h3>
            <div className="space-y-6">
               <div className="flex items-center gap-6 p-4 bg-white/5 rounded-2xl border border-white/10">
                  <div className="w-20 h-20 rounded-2xl overflow-hidden bg-black border border-white/10 relative group">
                     <img src={settings.creatorPic || '/logo.png'} className="w-full h-full object-cover" />
                     <button
                        onClick={() => { setUploadTarget('creator'); fileInputRef.current?.click(); }}
                        className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
                     >
                        <Upload size={20} />
                     </button>
                  </div>
                  <div className="flex-1">
                     <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-2 block">Developer Profile Photo URL</label>
                     <input
                        type="text"
                        value={settings.creatorPic}
                        onChange={(e) => setSettings({...settings, creatorPic: e.target.value})}
                        className="w-full bg-white/5 border border-white/10 p-3 rounded-xl focus:border-[#6C5CE7] outline-none text-xs"
                        placeholder="/logo.png or https://..."
                     />
                  </div>
               </div>
               <div>
                <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-3 block ml-1">GitHub Profile</label>
                <input
                  type="text"
                  value={settings.github}
                  onChange={(e) => setSettings({...settings, github: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 p-5 rounded-2xl focus:border-[#6C5CE7] outline-none transition-all font-medium"
                />
              </div>
              <div>
                <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-3 block ml-1">Contact Email</label>
                <input
                  type="email"
                  value={settings.email}
                  onChange={(e) => setSettings({...settings, email: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 p-5 rounded-2xl focus:border-[#6C5CE7] outline-none transition-all font-medium"
                />
              </div>
              <div>
                <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 mb-3 block ml-1">Developer Bio</label>
                <textarea
                  rows={4}
                  value={settings.bio}
                  onChange={(e) => setSettings({...settings, bio: e.target.value})}
                  className="w-full bg-white/5 border border-white/10 p-5 rounded-2xl focus:border-[#6C5CE7] outline-none resize-none transition-all font-medium leading-relaxed"
                />
              </div>

              {/* Security / Password Change */}
              <div className="pt-6 border-t border-white/5">
                <h4 className="text-sm font-black mb-6 flex items-center gap-3 italic uppercase text-red-500/80">
                  <Lock size={16} /> Security Access
                </h4>
                <div className="space-y-4">
                  <label className="text-[10px] font-black uppercase tracking-[0.4em] text-white/30 block ml-1">New Admin Password</label>
                  <input
                    type="text"
                    placeholder="Enter new password to change"
                    className="w-full bg-white/5 border border-white/10 p-4 rounded-xl focus:border-red-500/50 outline-none transition-all font-medium"
                    onChange={(e) => setSettings({...settings, adminKey: e.target.value})}
                  />
                  <p className="text-[9px] text-white/20 italic ml-1">* Leave empty to keep current password. Click 'Push Live Updates' to save.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Screenshots */}
          <div className="lg:col-span-2 bg-white/[0.02] border border-white/5 p-10 rounded-[3rem] space-y-8">
            <div className="flex justify-between items-center mb-6">
               <h3 className="text-2xl font-black flex items-center gap-4 italic uppercase">
                 <ImageIcon className="text-pink-500" /> App Showcase
               </h3>
               <div className="flex gap-4">
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    ref={fileInputRef}
                    onChange={handleFileUpload}
                  />
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="px-6 py-2 bg-[#6C5CE7]/10 hover:bg-[#6C5CE7]/20 border border-[#6C5CE7]/30 rounded-full text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2"
                  >
                    <Upload size={14} /> Upload from Gallery
                  </button>
                  <button
                    onClick={addScreenshot}
                    className="px-6 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-full text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2"
                  >
                    <Plus size={14} /> Add URL
                  </button>
               </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {settings.screenshots.map((url, idx) => (
                <div key={idx} className="space-y-2">
                   <label className="text-[9px] font-black uppercase tracking-widest text-indigo-400/60 ml-1">
                      {SCREEN_LABELS[idx] || `Extra Slide ${idx + 1}`}
                   </label>
                   <div className="flex gap-4 items-center group">
                      <div className="flex-1 relative">
                         <input
                           type="text"
                           value={url}
                           onChange={(e) => updateScreenshot(idx, e.target.value)}
                           placeholder="Image URL or local path"
                           className="w-full bg-white/5 border border-white/10 p-4 rounded-xl focus:border-[#6C5CE7] outline-none transition-all text-xs font-medium pr-12"
                         />
                         {url && (
                           <div className="absolute right-3 top-1/2 -translate-y-1/2 w-6 h-6 rounded-md overflow-hidden border border-white/10 bg-black">
                              <img src={url} alt="" className="w-full h-full object-cover" />
                           </div>
                         )}
                      </div>
                      <button
                        onClick={() => removeScreenshot(idx)}
                        className="p-4 bg-red-500/10 text-red-400 rounded-xl hover:bg-red-500 hover:text-white transition-all"
                      >
                        <Trash2 size={18} />
                      </button>
                   </div>
                </div>
              ))}
            </div>
            {settings.screenshots.length === 0 && (
              <p className="text-center py-10 text-white/10 font-black uppercase tracking-[0.5em]">No screenshots configured</p>
            )}
          </div>
        </div>

        {/* Action Bar */}
        <div className="fixed bottom-10 left-1/2 -translate-x-1/2 w-full max-w-5xl px-6">
           <button
            onClick={handleSave}
            disabled={isLoading}
            className="w-full bg-white text-black font-black py-8 rounded-[2.5rem] text-2xl flex items-center justify-center gap-4 hover:scale-[1.02] active:scale-[0.98] transition-all shadow-[0_40px_100px_rgba(255,255,255,0.1)] group"
          >
            {isLoading ? (
               <div className="w-6 h-6 border-4 border-black/20 border-t-black rounded-full animate-spin" />
            ) : saveStatus ? (
               <CheckCircle2 className="text-emerald-600" />
            ) : (
               <Save size={28} className="group-hover:rotate-12 transition-transform" />
            )}
            {saveStatus || 'PUSH LIVE UPDATES'}
          </button>
        </div>

        <div className="h-40" /> {/* Spacer */}
      </div>
    </div>
  );
}
