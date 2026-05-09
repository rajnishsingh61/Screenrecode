import React, { useState, useEffect } from 'react';
import { db } from './lib/firebase';
import { doc, getDoc, setDoc, collection, onSnapshot, updateDoc } from 'firebase/firestore';
import { Settings, Bell, Image as ImageIcon, Users, CreditCard, Check, X, Save } from 'lucide-react';

export default function AdminPanel() {
  const [config, setConfig] = useState({
    updateOverlayVisible: false,
    updateMessage: '',
    notificationTitle: '',
    notificationBody: '',
    overlayAdUrl: '',
    overlayAdVisible: false,
    premiumPrice: '₹499',
    qrCodeUrl: ''
  });

  const [requests, setRequests] = useState([]);
  const [activeTab, setActiveTab] = useState('config');

  useEffect(() => {
    // Load config
    const loadConfig = async () => {
      const docRef = doc(db, 'app_config', 'settings');
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) setConfig(docSnap.data());
    };
    loadConfig();

    // Listen to premium requests
    const unsubscribe = onSnapshot(collection(db, 'premium_requests'), (snapshot) => {
      const reqs = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setRequests(reqs.filter(r => r.status === 'pending'));
    });

    return () => unsubscribe();
  }, []);

  const saveConfig = async () => {
    try {
      await setDoc(doc(db, 'app_config', 'settings'), config);
      alert('Configuration saved successfully!');
    } catch (error) {
      alert('Error saving config: ' + error.message);
    }
  };

  const handleRequest = async (id, userId, approve) => {
    try {
      await updateDoc(doc(db, 'premium_requests', id), {
        status: approve ? 'approved' : 'rejected'
      });
      if (approve) {
        await updateDoc(doc(db, 'users', userId), { isPremium: true });
      }
      alert(`Request ${approve ? 'approved' : 'rejected'}`);
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-black text-gray-900 mb-8 flex items-center gap-3">
          <Settings className="w-8 h-8 text-purple-600" />
          GameRec Pro Admin
        </h1>

        <div className="flex gap-4 mb-8">
          <button 
            onClick={() => setActiveTab('config')}
            className={`px-6 py-2 rounded-full font-bold transition-all ${activeTab === 'config' ? 'bg-purple-600 text-white shadow-lg' : 'bg-white text-gray-600'}`}
          >
            Config
          </button>
          <button 
            onClick={() => setActiveTab('requests')}
            className={`px-6 py-2 rounded-full font-bold transition-all ${activeTab === 'requests' ? 'bg-purple-600 text-white shadow-lg' : 'bg-white text-gray-600'}`}
          >
            Premium Requests ({requests.length})
          </button>
        </div>

        {activeTab === 'config' && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Bell className="w-5 h-5 text-blue-500" /> Update & Notifications
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <label className="text-sm font-medium w-32">Overlay Visible</label>
                  <input 
                    type="checkbox" 
                    checked={config.updateOverlayVisible}
                    onChange={e => setConfig({...config, updateOverlayVisible: e.target.checked})}
                    className="w-5 h-5 rounded"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Update Message</label>
                  <input 
                    type="text" 
                    value={config.updateMessage}
                    onChange={e => setConfig({...config, updateMessage: e.target.value})}
                    className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl"
                  />
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <ImageIcon className="w-5 h-5 text-green-500" /> Overlay Ads
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <label className="text-sm font-medium w-32">Show Ad</label>
                  <input 
                    type="checkbox" 
                    checked={config.overlayAdVisible}
                    onChange={e => setConfig({...config, overlayAdVisible: e.target.checked})}
                    className="w-5 h-5 rounded"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Image URL</label>
                  <input 
                    type="text" 
                    value={config.overlayAdUrl}
                    onChange={e => setConfig({...config, overlayAdUrl: e.target.value})}
                    className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl"
                  />
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-orange-500" /> Payment Settings
              </h2>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Premium Price</label>
                  <input 
                    type="text" 
                    value={config.premiumPrice}
                    onChange={e => setConfig({...config, premiumPrice: e.target.value})}
                    className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">QR Code Image URL</label>
                  <input 
                    type="text" 
                    value={config.qrCodeUrl}
                    onChange={e => setConfig({...config, qrCodeUrl: e.target.value})}
                    className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl"
                  />
                </div>
              </div>
            </div>

            <button 
              onClick={saveConfig}
              className="w-full py-4 bg-purple-600 text-white rounded-2xl font-black shadow-xl shadow-purple-200 flex items-center justify-center gap-2 hover:bg-purple-700 transition-all"
            >
              <Save className="w-5 h-5" /> SAVE CONFIGURATION
            </button>
          </div>
        )}

        {activeTab === 'requests' && (
          <div className="space-y-4">
            {requests.length === 0 ? (
              <div className="bg-white p-12 text-center rounded-3xl text-gray-400 font-medium">
                No pending requests
              </div>
            ) : (
              requests.map(req => (
                <div key={req.id} className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex items-center justify-between">
                  <div>
                    <p className="font-bold text-gray-900">{req.userEmail}</p>
                    <p className="text-xs text-gray-400">UID: {req.userId}</p>
                    <p className="text-xs text-purple-600 font-bold mt-1">TXN: {req.transactionId || 'N/A'}</p>
                  </div>
                  <div className="flex gap-2">
                    <button 
                      onClick={() => handleRequest(req.id, req.userId, true)}
                      className="p-3 bg-green-50 text-green-600 rounded-2xl hover:bg-green-100 transition-all"
                    >
                      <Check className="w-6 h-6" />
                    </button>
                    <button 
                      onClick={() => handleRequest(req.id, req.userId, false)}
                      className="p-3 bg-red-50 text-red-600 rounded-2xl hover:bg-red-100 transition-all"
                    >
                      <X className="w-6 h-6" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
