/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { 
  Monitor, 
  Smartphone, 
  Download, 
  Settings, 
  Activity, 
  Play, 
  Video, 
  Loader2,
  Lock,
  ChevronRight,
  ShieldCheck,
  Zap,
  Globe
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import AdminPanel from './AdminPanel';

export default function App() {
  const [isLoading, setIsLoading] = React.useState(true);
  const [isLoggedIn, setIsLoggedIn] = React.useState(false);
  const [isAdmin, setIsAdmin] = React.useState(false);

  React.useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 2000);
    return () => clearTimeout(timer);
  }, []);

  const handleLogin = () => {
    setIsLoggedIn(true);
    alert("Logged in with Google (Preview)");
  };

  if (isAdmin) {
    return (
      <div className="relative">
        <button 
          onClick={() => setIsAdmin(false)}
          className="fixed top-4 right-4 z-[9999] px-4 py-2 bg-black text-white text-[10px] font-black rounded-full shadow-2xl hover:scale-105 active:scale-95 transition-all"
        >
          EXIT ADMIN MODE
        </button>
        <AdminPanel />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F0F2F5] text-[#1C1E21] font-sans selection:bg-purple-500/20 overflow-hidden flex flex-col">
      {/* Header */}
      <header className="h-16 flex items-center px-6 justify-between flex-shrink-0 bg-white shadow-sm z-50">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-black text-[#A020F0] tracking-widest italic group">GAMEREC <span className="text-[#1C1E21]">PRO</span></h1>
        </div>
        <div className="flex items-center gap-4">
          {!isLoggedIn ? (
            <button 
              onClick={handleLogin}
              className="flex items-center gap-2 px-4 py-1.5 bg-white border border-gray-200 rounded-full text-xs font-bold hover:bg-gray-50 transition-colors shadow-sm"
            >
              <img src="https://www.google.com/favicon.ico" className="w-3 h-3" alt="Google" />
              GOOGLE LOGIN
            </button>
          ) : (
            <div className="flex items-center gap-2 px-3 py-1 bg-purple-50 text-purple-600 border border-purple-100 rounded-full text-[10px] font-bold">
              <div className="w-4 h-4 bg-purple-200 rounded-full" />
              HI, GAMER
            </div>
          )}
          <button 
            onClick={() => setIsAdmin(true)}
            className="p-2 text-[#606770] hover:bg-black/5 rounded-full transition-colors active:scale-90"
          >
            <Lock className="w-5 h-5" />
          </button>
          <button className="p-2 text-[#606770] hover:bg-black/5 rounded-full transition-colors active:scale-90">
            <Smartphone className="w-5 h-5" />
          </button>
          <button className="px-4 py-1.5 bg-gradient-to-r from-[#A020F0] to-[#E020E0] text-white text-xs font-black rounded-full flex items-center gap-2 shadow-lg shadow-purple-500/20 active:scale-95 transition-all">
            <Download className="w-3.5 h-3.5" />
            PREMIUM
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 overflow-y-auto px-6 py-6 max-w-2xl mx-auto w-full">
        <AnimatePresence mode="wait">
          {isLoading ? (
            <motion.div
              key="skeleton"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="space-y-8"
            >
              {/* Skeleton Performance Card */}
              <div className="bg-white border border-black/5 rounded-3xl p-6 shadow-sm animate-pulse">
                <div className="flex justify-between items-start mb-6">
                  <div className="space-y-2">
                    <div className="w-32 h-3 bg-gray-200 rounded-full" />
                    <div className="w-48 h-6 bg-gray-200 rounded-full" />
                  </div>
                  <div className="w-10 h-10 bg-gray-200 rounded-xl" />
                </div>
                <div className="w-full h-4 bg-gray-200 rounded-full mb-6" />
                <div className="flex gap-4">
                  <div className="flex-1 h-16 bg-gray-100 rounded-2xl" />
                  <div className="flex-1 h-16 bg-gray-100 rounded-2xl" />
                </div>
              </div>

              {/* Skeleton Grid */}
              <div className="grid grid-cols-2 gap-4">
                <div className="aspect-square bg-white border border-black/5 rounded-3xl animate-pulse" />
                <div className="aspect-square bg-white border border-black/5 rounded-3xl animate-pulse" />
              </div>
            </motion.div>
          ) : (
            <motion.div
              key="content"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="pb-32"
            >
              {/* Performance Card */}
              <div className="bg-white border border-black/5 rounded-3xl p-6 mb-8 relative overflow-hidden shadow-sm shadow-black/5 group">
                <div className="absolute top-0 right-0 w-32 h-32 bg-[#A020F0]/5 blur-3xl -mr-16 -mt-16" />
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <p className="text-[#007AFF] text-[10px] font-black tracking-widest mb-1 uppercase">Game Engine Optimization</p>
                    <h2 className="text-2xl font-bold text-[#1C1E21] leading-tight">Smooth Recording</h2>
                  </div>
                  <div className="bg-[#A020F0]/10 p-2 rounded-xl">
                    <Activity className="w-6 h-6 text-[#A020F0]" />
                  </div>
                </div>
                <p className="text-[#606770] text-sm leading-relaxed mb-6">
                  Capturing gameplay at high-fidelity 1080p 60fps with native hardware acceleration and low battery overhead.
                </p>
                <div className="flex gap-4">
                   <div className="flex-1 bg-[#F0F2F5] rounded-2xl p-4 border border-black/5">
                      <p className="text-[10px] text-[#606770] font-bold mb-1">FPS STATUS</p>
                      <p className="text-lg font-black text-[#1C1E21]">60.0 <span className="text-[10px] text-green-600">STABLE</span></p>
                   </div>
                   <div className="flex-1 bg-[#F0F2F5] rounded-2xl p-4 border border-black/5">
                      <p className="text-[10px] text-[#606770] font-bold mb-1">BITRATE</p>
                      <p className="text-lg font-black text-[#1C1E21]">12.5 <span className="text-[10px] text-blue-600">MBPS</span></p>
                   </div>
                </div>
              </div>

              {/* Action Grid */}
              <div className="grid grid-cols-2 gap-4 mb-12">
                 <div 
                    onClick={() => alert("Dashboard Active - Syncing stats...")}
                    className="aspect-square bg-white border border-black/5 rounded-3xl flex flex-col items-center justify-center gap-3 hover:bg-[#F0F2F5] transition-all cursor-pointer shadow-sm shadow-black/5 group active:scale-95"
                 >
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#A020F0] to-[#7B12FF] grid place-items-center group-hover:scale-110 transition-transform">
                      <Play className="w-6 h-6 text-white" />
                    </div>
                    <span className="font-bold text-sm text-[#1C1E21]">Dashboard</span>
                 </div>
                 <div 
                    onClick={() => alert("Opening Recordings List...")}
                    className="aspect-square bg-white border border-black/5 rounded-3xl flex flex-col items-center justify-center gap-3 hover:bg-[#F0F2F5] transition-all cursor-pointer shadow-sm shadow-black/5 group active:scale-95"
                 >
                    <div className="w-12 h-12 rounded-2xl bg-[#F0F2F5] border border-black/5 grid place-items-center group-hover:scale-110 transition-transform">
                      <Video className="w-6 h-6 text-[#606770]" />
                    </div>
                    <span className="font-bold text-sm text-[#606770]">Recordings</span>
                 </div>
              </div>

              {/* Main CTA */}
              <div className="fixed bottom-32 left-0 right-0 px-6 flex flex-col items-center">
                <button 
                  onClick={() => alert("Initializing Recording Engine...")}
                  className="w-full max-w-sm h-16 bg-gradient-to-r from-[#A020F0] to-[#7B12FF] hover:brightness-110 text-white rounded-2xl font-black text-lg flex items-center justify-center gap-4 shadow-xl shadow-purple-500/20 transition-all active:scale-95 group"
                >
                  <Settings className="w-6 h-6 group-hover:rotate-90 transition-transform duration-500" />
                  INITIALIZE ENGINE
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Global Floating Indicator Mockup */}
        <div className="fixed top-1/2 right-6 -translate-y-1/2 animate-bounce-slow z-50">
           <div className="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow-xl shadow-black/10 border border-black/5">
              <Activity className="w-5 h-5 text-[#A020F0]" />
           </div>
        </div>

        {/* Bottom Nav Bar */}
        <div className="fixed bottom-0 left-0 right-0 h-20 bg-white/90 backdrop-blur-xl border-t border-black/5 flex items-center px-12 justify-around z-50">
            <button className="p-3 text-[#A020F0] transition-colors"><Monitor className="w-6 h-6" /></button>
            <button className="p-3 text-[#606770] hover:text-[#A020F0] transition-colors"><Settings className="w-6 h-6" /></button>
        </div>
      </main>
    </div>
  );
}

