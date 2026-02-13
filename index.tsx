import React, { useState, useEffect, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import { Clock, CheckCircle, XCircle, Download, FileJson, Tablet } from 'lucide-react';

const KioskSimulator = () => {
  const [status, setStatus] = useState<'FREE' | 'BUSY'>('FREE');
  const [endTime, setEndTime] = useState<number | null>(null);
  const [tapCount, setTapCount] = useState(0);
  const [showConfirm, setShowConfirm] = useState<{ visible: boolean; minutes: number }>({ visible: false, minutes: 0 });
  const tapTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Persistence and State Restoration
  useEffect(() => {
    const storedEndTime = localStorage.getItem('kiosk_end_time');
    if (storedEndTime) {
      const end = parseInt(storedEndTime, 10);
      if (Date.now() < end) {
        setEndTime(end);
        setStatus('BUSY');
      } else {
        localStorage.removeItem('kiosk_end_time');
      }
    }
  }, []);

  // Timer loop
  useEffect(() => {
    const interval = setInterval(() => {
      if (endTime) {
        if (Date.now() >= endTime) {
          setEndTime(null);
          setStatus('FREE');
          localStorage.removeItem('kiosk_end_time');
        }
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [endTime]);

  const handleBooking = (minutes: number) => {
    setShowConfirm({ visible: true, minutes });
  };

  const confirmBooking = () => {
    const durationMs = showConfirm.minutes * 60 * 1000;
    // For demo purposes, we can use seconds instead of minutes to see it change faster
    // But the prompt implies minutes. Let's stick to minutes but maybe 15 seconds for testing?
    // No, let's stick to real time for accuracy to the "native app" requirement.
    const newEndTime = Date.now() + durationMs;
    
    setEndTime(newEndTime);
    setStatus('BUSY');
    localStorage.setItem('kiosk_end_time', newEndTime.toString());
    setShowConfirm({ visible: false, minutes: 0 });
  };

  const handleAdminTap = () => {
    const now = Date.now();
    
    if (tapTimeoutRef.current) {
      clearTimeout(tapTimeoutRef.current);
    }

    setTapCount(prev => {
      const newCount = prev + 1;
      if (newCount >= 5) {
        // Toggle logic
        if (status === 'FREE') {
           // Simulate a test booking
           const newEnd = Date.now() + 15 * 60 * 1000;
           setEndTime(newEnd);
           localStorage.setItem('kiosk_end_time', newEnd.toString());
           setStatus('BUSY');
        } else {
          setEndTime(null);
          localStorage.removeItem('kiosk_end_time');
          setStatus('FREE');
        }
        return 0;
      }
      return newCount;
    });

    tapTimeoutRef.current = setTimeout(() => {
      setTapCount(0);
    }, 500);
  };

  return (
    <div className="min-h-screen bg-gray-900 flex flex-col font-sans">
      {/* Header / Info for the Web Preview */}
      <header className="bg-gray-800 text-white p-4 border-b border-gray-700 flex justify-between items-center">
        <div className="flex items-center space-x-2">
           <Tablet className="w-6 h-6 text-blue-400" />
           <h1 className="text-xl font-bold">Android Kiosk Simulator</h1>
        </div>
        <div className="text-sm text-gray-400">
          Native Android files generated in project root
        </div>
      </header>

      {/* Simulator Container */}
      <div className="flex-grow flex items-center justify-center p-8">
        {/* Device Frame */}
        <div className="relative w-full max-w-4xl aspect-[16/10] bg-black rounded-[2rem] shadow-2xl overflow-hidden border-8 border-gray-800 ring-4 ring-gray-900">
          
          {/* App Screen Content */}
          <div 
            className={`w-full h-full flex flex-col items-center justify-center transition-colors duration-500 ease-in-out ${
              status === 'FREE' ? 'bg-emerald-700' : 'bg-red-800'
            }`}
          >
            {/* Status Text / Admin Touch Target */}
            <div 
              onClick={handleAdminTap}
              className="cursor-pointer select-none active:scale-95 transition-transform"
            >
              <h1 className="text-white text-6xl md:text-8xl font-bold tracking-tight text-center drop-shadow-md">
                {status === 'FREE' ? 'Book Room' : 'Occupied'}
              </h1>
              {status === 'BUSY' && endTime && (
                <p className="text-white/80 text-xl md:text-2xl text-center mt-4 font-medium">
                  Free at {new Date(endTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                </p>
              )}
            </div>

            {/* Action Buttons (Only in FREE state) */}
            {status === 'FREE' && (
              <div className="mt-16 flex flex-row gap-8">
                {[15, 30, 60].map((mins) => (
                  <button
                    key={mins}
                    onClick={() => handleBooking(mins)}
                    className="w-32 h-32 md:w-40 md:h-40 border-4 border-white/30 bg-white/10 rounded-2xl flex flex-col items-center justify-center hover:bg-white/20 active:scale-95 transition-all text-white backdrop-blur-sm"
                  >
                    <span className="text-4xl md:text-5xl font-bold">{mins}</span>
                    <span className="text-sm md:text-base font-medium uppercase tracking-wider mt-1">minutes</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Modal Overlay */}
          {showConfirm.visible && (
             <div className="absolute inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 animate-in fade-in duration-200">
               <div className="bg-white text-gray-900 p-8 rounded-xl shadow-2xl max-w-sm w-full mx-4 transform transition-all scale-100">
                  <h3 className="text-2xl font-bold mb-4">Confirm Booking</h3>
                  <p className="text-lg text-gray-600 mb-8">
                    Book the room for <span className="font-bold text-emerald-600">{showConfirm.minutes} minutes</span>?
                  </p>
                  <div className="flex justify-end gap-4">
                    <button 
                      onClick={() => setShowConfirm({ visible: false, minutes: 0 })}
                      className="px-6 py-3 text-gray-600 font-bold hover:bg-gray-100 rounded-lg transition-colors"
                    >
                      CANCEL
                    </button>
                    <button 
                      onClick={confirmBooking}
                      className="px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow-lg transition-colors"
                    >
                      BOOK
                    </button>
                  </div>
               </div>
             </div>
          )}

        </div>
      </div>
      
      <div className="bg-gray-800 text-gray-400 text-xs p-2 text-center">
        Debug: Tap the status title 5 times quickly to toggle Free/Busy state.
      </div>
    </div>
  );
};

const root = createRoot(document.getElementById('root')!);
root.render(<KioskSimulator />);