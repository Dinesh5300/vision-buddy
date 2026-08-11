// Vision Buddy AI Glass - Web Interactive Simulator Logic

document.addEventListener('DOMContentLoaded', () => {
    // Navigation & Screen Elements
    const screens = {
        home: document.getElementById('screen-home'),
        camera: document.getElementById('screen-camera'),
        voice: document.getElementById('screen-voice'),
        sos: document.getElementById('screen-sos')
    };

    const navButtons = document.querySelectorAll('[data-navigate]');
    const btnBackFromCam = document.getElementById('btnBackFromCam');

    // Camera Screen Elements
    const camScreenTitle = document.getElementById('camScreenTitle');
    const sampleFeedSelect = document.getElementById('sampleFeedSelect');
    const videoElem = document.getElementById('webcamVideo');
    const canvasElem = document.getElementById('detectionCanvas');
    const ctx = canvasElem.getContext('2d');
    const spokenTextElem = document.getElementById('spokenText');
    const spokenBubble = document.getElementById('spokenBubble');
    const resText = document.getElementById('resText');
    const resObjects = document.getElementById('resObjects');
    const modelTag = document.getElementById('modelTag');
    const triggerBtnText = document.getElementById('triggerBtnText');
    const liveStatusTag = document.getElementById('liveStatusTag');
    const aiModelText = document.getElementById('aiModelText');
    const aiModelPill = document.getElementById('aiModelPill');

    // Image Statement Card Elements
    const sampleTitleTag = document.getElementById('sampleTitleTag');
    const sampleStatementText = document.getElementById('sampleStatementText');
    const detailContext = document.getElementById('detailContext');
    const detailTarget = document.getElementById('detailTarget');
    const detailGuidance = document.getElementById('detailGuidance');

    // Control Buttons
    const btnManualTrigger = document.getElementById('btnManualTrigger');
    const btnToggleSpeech = document.getElementById('btnToggleSpeech');
    const btnToggleCamera = document.getElementById('btnToggleCamera');
    const toggleThemeBtn = document.getElementById('toggleThemeBtn');
    const muteIcon = document.getElementById('muteIcon');

    // Voice Screen Elements
    const micOrb = document.getElementById('micOrb');
    const voicePromptText = document.getElementById('voicePromptText');
    const voiceHeardText = document.getElementById('voiceHeardText');
    const commandChips = document.querySelectorAll('.command-chip');

    // SOS Elements
    const btnTriggerSOS = document.getElementById('btnTriggerSOS');
    const sosStatusText = document.getElementById('sosStatusText');

    // App State
    let currentMode = 'assist'; // 'assist', 'read', 'detect'
    let isMuted = false;
    let currentFeed = 'bus_stop';
    let isWebcamActive = false;
    let mediaStream = null;
    let animationFrameId = null;
    let lastSpokenMessage = '';
    
    // AI Engines State
    let cocoModel = null;
    let isTfModelLoaded = false;
    let isOcrAnalyzing = false;
    let liveWebcamDetections = [];
    let liveWebcamOcrText = '';
    let lastOcrTime = 0;

    // Load TensorFlow COCO-SSD Model for Real-Time Webcam Detection
    async function loadAiModels() {
        aiModelText.innerText = 'AI: Loading...';
        try {
            if (window.cocoSsd) {
                cocoModel = await window.cocoSsd.load();
                isTfModelLoaded = true;
                aiModelText.innerText = 'AI: Ready (COCO-SSD)';
                aiModelPill.classList.add('active');
                console.log('TensorFlow COCO-SSD Model Loaded!');
            } else {
                aiModelText.innerText = 'AI: Simulated';
            }
        } catch (e) {
            console.warn('COCO-SSD load error:', e);
            aiModelText.innerText = 'AI: Fallback Engine';
        }
    }
    loadAiModels();

    // Sample Feeds Data (Comprehensive Scenes with Image Details & Statements)
    const feedsData = {
        bus_stop: {
            title: 'Bus Stop & Route Sign',
            ocrText: 'BUS STOP — Route 42 to Downtown. Next Bus: 5 mins.',
            statement: 'A bus stop sign is visible ahead displaying Route 42 to Downtown. 1 person and a bench are positioned nearby.',
            context: 'Outdoor Transit Station',
            target: 'Bus Stop Sign (94% confidence)',
            guidance: 'Path clear. Bench 2 meters to the right.',
            objects: [
                { label: 'bus stop sign', confidence: 0.94, bbox: [0.3, 0.15, 0.4, 0.45] },
                { label: 'person', confidence: 0.88, bbox: [0.08, 0.38, 0.22, 0.55] },
                { label: 'bench', confidence: 0.76, bbox: [0.55, 0.6, 0.38, 0.32] }
            ],
            color: '#1e293b'
        },
        desk: {
            title: 'Office Workspace',
            ocrText: 'VISION BUDDY AI MANUAL v1.0 — On-Device Vision Engine',
            statement: 'An office workstation with an open laptop, a ceramic coffee mug on the right, and a smartphone on the left.',
            context: 'Indoor Office Desk',
            target: 'Laptop Computer (97% confidence)',
            guidance: 'Hot mug on right side. Phone near keyboard.',
            objects: [
                { label: 'laptop', confidence: 0.97, bbox: [0.22, 0.28, 0.52, 0.48] },
                { label: 'cup', confidence: 0.92, bbox: [0.76, 0.52, 0.18, 0.32] },
                { label: 'cell phone', confidence: 0.88, bbox: [0.06, 0.58, 0.16, 0.28] },
                { label: 'chair', confidence: 0.84, bbox: [0.72, 0.18, 0.24, 0.35] }
            ],
            color: '#0f172a'
        },
        store: {
            title: 'Coffee Shop Entrance',
            ocrText: 'COFFEE SHOP & BAKERY — OPEN 24/7. FRESH ESPRESSO & PASTRIES.',
            statement: 'Storefront entrance for Coffee Shop & Bakery with open hours sign. Glass doors are clear.',
            context: 'Commercial Storefront',
            target: 'Store Entrance Sign (95% confidence)',
            guidance: 'Automatic glass doors 3 meters ahead.',
            objects: [
                { label: 'store sign', confidence: 0.95, bbox: [0.18, 0.1, 0.64, 0.35] },
                { label: 'door', confidence: 0.91, bbox: [0.32, 0.42, 0.36, 0.52] },
                { label: 'cup', confidence: 0.89, bbox: [0.75, 0.68, 0.14, 0.24] }
            ],
            color: '#1e1b4b'
        },
        street: {
            title: 'City Crosswalk & Signal',
            ocrText: 'WALK — CROSS WITH CAUTION WHEN SIGNAL TURNS GREEN',
            statement: 'Urban pedestrian crossing. Traffic signal shows WALK icon. A sedan car is stopped at the line.',
            context: 'City Street Intersection',
            target: 'Pedestrian Signal (96% confidence)',
            guidance: 'Safe to cross. Watch for turning vehicles.',
            objects: [
                { label: 'traffic light', confidence: 0.96, bbox: [0.74, 0.08, 0.18, 0.38] },
                { label: 'car', confidence: 0.91, bbox: [0.48, 0.45, 0.42, 0.42] },
                { label: 'person', confidence: 0.87, bbox: [0.15, 0.35, 0.16, 0.52] },
                { label: 'stop sign', confidence: 0.94, bbox: [0.04, 0.15, 0.18, 0.32] }
            ],
            color: '#111827'
        },
        book: {
            title: 'Book & Document Page',
            ocrText: 'CHAPTER 1: Introduction to Vision AI for Visually Impaired Assistance. Camera feeds stream directly to on-device neural decoders.',
            statement: 'Open textbook page titled Chapter 1 with technical paragraphs detailing camera vision assistance.',
            context: 'Document Reading',
            target: 'Page Title & Text (98% confidence)',
            guidance: 'Hold camera steady 30cm above page.',
            objects: [
                { label: 'book', confidence: 0.98, bbox: [0.12, 0.12, 0.76, 0.76] }
            ],
            color: '#311b92'
        }
    };

    // --- Voice Synthesis Engine (TTS) ---
    function speak(text, force = false) {
        if (!text || (isMuted && !force)) return;
        
        lastSpokenMessage = text;
        spokenTextElem.innerText = text;

        spokenBubble.style.borderColor = '#3b82f6';
        spokenBubble.style.transform = 'scale(1.03)';
        setTimeout(() => spokenBubble.style.transform = 'scale(1)', 200);

        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.rate = 1.0;
            utterance.pitch = 1.0;
            window.speechSynthesis.speak(utterance);
        }
    }

    // --- Navigation Logic ---
    function navigateTo(screenName, mode = 'assist') {
        currentMode = mode;
        Object.values(screens).forEach(s => s.classList.remove('active'));

        if (screenName === 'home') {
            screens.home.classList.add('active');
            stopCamera();
            speak('Home screen');
        } else if (screenName === 'voice') {
            screens.voice.classList.add('active');
            stopCamera();
            speak('Voice command mode. Say a command or click a chip.');
        } else if (screenName === 'sos') {
            screens.sos.classList.add('active');
            stopCamera();
            speak('Emergency SOS mode');
        } else {
            screens.camera.classList.add('active');
            setupCameraMode(mode);
        }
    }

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const dest = btn.getAttribute('data-navigate');
            navigateTo(dest, dest);
        });
    });

    btnBackFromCam.addEventListener('click', () => navigateTo('home'));

    // --- Camera & Vision Engine Setup ---
    function setupCameraMode(mode) {
        if (mode === 'assist') {
            camScreenTitle.innerText = 'Start Assistance (Combined OCR + YOLO)';
            modelTag.innerText = isWebcamActive ? 'TF.js COCO-SSD + Tesseract' : 'ML Kit OCR + YOLOv5n';
            triggerBtnText.innerText = 'Announce Vision';
            speak('Assistance mode activated. Scanning feed for text and objects.');
        } else if (mode === 'read') {
            camScreenTitle.innerText = 'Read Text Mode (OCR Focus)';
            modelTag.innerText = isWebcamActive ? 'Tesseract.js Live OCR' : 'ML Kit Text Recognition';
            triggerBtnText.innerText = 'Read Aloud';
            speak('Read Text mode. Point camera directly at text.');
        } else if (mode === 'detect') {
            camScreenTitle.innerText = 'Detect Object Mode (YOLO / COCO AI)';
            modelTag.innerText = isWebcamActive ? 'TF.js COCO-SSD Real-Time' : 'YOLOv5n TFLite (80 Classes)';
            triggerBtnText.innerText = 'Announce Objects';
            speak('Detect Object mode. Scanning for objects.');
        }

        updateStatementCard();
        renderLoop();
    }

    sampleFeedSelect.addEventListener('change', (e) => {
        currentFeed = e.target.value;
        if (currentFeed === 'webcam') {
            startWebcam();
        } else {
            stopWebcam();
            updateStatementCard();
            renderLoop();
        }
    });

    // Update Image Details & Statement Card according to selected feed
    function updateStatementCard() {
        if (isWebcamActive) {
            sampleTitleTag.innerText = '📷 Live Camera Stream';
            sampleStatementText.innerText = `Real-time live camera analysis running. ${liveWebcamDetections.length} objects detected in frame. ${liveWebcamOcrText ? 'Text detected in view.' : 'Searching for text...'}`;
            detailContext.innerText = 'Live Real-World Environment';
            detailTarget.innerText = liveWebcamDetections.length ? `${liveWebcamDetections[0].class} (${(liveWebcamDetections[0].score*100).toFixed(0)}%)` : 'Scanning...';
            detailGuidance.innerText = 'Point camera toward items or text for instant readout.';
            return;
        }

        const data = feedsData[currentFeed] || feedsData.bus_stop;
        sampleTitleTag.innerText = data.title;
        sampleStatementText.innerText = data.statement;
        detailContext.innerText = data.context;
        detailTarget.innerText = data.target;
        detailGuidance.innerText = data.guidance;
    }

    // --- Webcam Controls & Real-Time AI Detection ---
    async function startWebcam() {
        try {
            mediaStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } } });
            videoElem.srcObject = mediaStream;
            videoElem.classList.remove('hidden');
            isWebcamActive = true;
            document.getElementById('camPillText').innerText = 'Cam: Live';
            document.getElementById('camPill').classList.add('active');
            liveStatusTag.innerHTML = '<span class="blink-dot"></span> LIVE WEBCAM AI';
            modelTag.innerText = 'TF.js COCO-SSD + Tesseract';
            
            updateStatementCard();
            speak('Live webcam activated. Running real-time computer vision.');
        } catch (err) {
            console.warn('Webcam access error:', err);
            speak('Webcam unavailable or permission denied. Loading Bus Stop sample feed.');
            sampleFeedSelect.value = 'bus_stop';
            currentFeed = 'bus_stop';
            stopWebcam();
        }
    }

    function stopWebcam() {
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.stop());
            mediaStream = null;
        }
        videoElem.classList.add('hidden');
        isWebcamActive = false;
        document.getElementById('camPillText').innerText = 'Cam: Sample';
        document.getElementById('camPill').classList.remove('active');
        liveStatusTag.innerHTML = '<span class="blink-dot"></span> SAMPLE AI RUNNING';
    }

    function stopCamera() {
        stopWebcam();
        if (animationFrameId) {
            cancelAnimationFrame(animationFrameId);
            animationFrameId = null;
        }
    }

    // --- Real-Time Object Detection on Live Webcam Frame ---
    async function processLiveWebcamAi() {
        if (!isWebcamActive || videoElem.readyState !== 4) return;

        // 1. Run COCO-SSD Object Detection on video element
        if (isTfModelLoaded && cocoModel && (currentMode === 'assist' || currentMode === 'detect')) {
            try {
                const predictions = await cocoModel.detect(videoElem);
                liveWebcamDetections = predictions.filter(p => p.score >= 0.45);
            } catch (e) {
                console.warn('Live detection frame error:', e);
            }
        }

        // 2. Run Periodic Live OCR Reading (every 3 seconds)
        const now = Date.now();
        if ((currentMode === 'assist' || currentMode === 'read') && window.Tesseract && !isOcrAnalyzing && now - lastOcrTime > 3000) {
            isOcrAnalyzing = true;
            lastOcrTime = now;

            try {
                // Create temporary frame canvas for OCR
                const tempCanvas = document.createElement('canvas');
                tempCanvas.width = videoElem.videoWidth || 320;
                tempCanvas.height = videoElem.videoHeight || 240;
                const tempCtx = tempCanvas.getContext('2d');
                tempCtx.drawImage(videoElem, 0, 0, tempCanvas.width, tempCanvas.height);

                window.Tesseract.recognize(tempCanvas, 'eng', { logger: () => {} })
                    .then(res => {
                        const clean = (res.data.text || '').trim().replace(/\n+/g, ' ');
                        if (clean.length > 3) {
                            liveWebcamOcrText = clean;
                        }
                    })
                    .catch(err => console.warn('Tesseract OCR error:', err))
                    .finally(() => { isOcrAnalyzing = false; });
            } catch (e) {
                isOcrAnalyzing = false;
            }
        }

        updateStatementCard();
    }

    // --- Main Rendering Loop (Webcam vs Sample Feed Graphics) ---
    function renderLoop() {
        canvasElem.width = canvasElem.clientWidth || 600;
        canvasElem.height = canvasElem.clientHeight || 340;

        ctx.clearRect(0, 0, canvasElem.width, canvasElem.height);

        if (isWebcamActive) {
            // Render Live Webcam Video onto Canvas
            ctx.drawImage(videoElem, 0, 0, canvasElem.width, canvasElem.height);
            
            // Process AI predictions on webcam frame asynchronously
            processLiveWebcamAi();

            // Render Real-Time Bounding Boxes from COCO-SSD
            if (currentMode === 'assist' || currentMode === 'detect') {
                liveWebcamDetections.forEach((item, idx) => {
                    const [vx, vy, vw, vh] = item.bbox;
                    const scaleX = canvasElem.width / (videoElem.videoWidth || 640);
                    const scaleY = canvasElem.height / (videoElem.videoHeight || 480);

                    const x = vx * scaleX;
                    const y = vy * scaleY;
                    const w = vw * scaleX;
                    const h = vh * scaleY;

                    ctx.strokeStyle = idx === 0 ? '#10b981' : '#3b82f6';
                    ctx.lineWidth = 3;
                    ctx.strokeRect(x, y, w, h);

                    const tagStr = `${item.class} ${(item.score * 100).toFixed(0)}%`;
                    ctx.font = 'bold 12px JetBrains Mono, monospace';
                    const tw = ctx.measureText(tagStr).width;

                    ctx.fillStyle = idx === 0 ? 'rgba(16, 185, 129, 0.9)' : 'rgba(59, 130, 246, 0.9)';
                    ctx.fillRect(x, y - 24 > 0 ? y - 24 : y, tw + 12, 24);

                    ctx.fillStyle = '#ffffff';
                    ctx.fillText(tagStr, x + 6, (y - 24 > 0 ? y - 8 : y + 16));
                });
            }

            // Update Perception Log UI for Webcam
            if (currentMode === 'read') {
                resText.innerText = liveWebcamOcrText || 'Scanning webcam for text...';
                resObjects.innerHTML = '<span class="tag-empty">YOLO paused in Read mode</span>';
            } else if (currentMode === 'detect') {
                resText.innerText = 'OCR paused in Detect mode';
                resObjects.innerHTML = liveWebcamDetections.length ? liveWebcamDetections.map(o => 
                    `<span class="tag-item">${o.class} (${(o.score * 100).toFixed(0)}%)</span>`
                ).join('') : '<span class="tag-empty">Point camera at objects...</span>';
            } else {
                resText.innerText = liveWebcamOcrText || 'Scanning webcam for text...';
                resObjects.innerHTML = liveWebcamDetections.length ? liveWebcamDetections.map(o => 
                    `<span class="tag-item">${o.class} (${(o.score * 100).toFixed(0)}%)</span>`
                ).join('') : '<span class="tag-empty">Scanning for objects...</span>';
            }

        } else {
            // Render High Quality Sample Scene Graphics
            const data = feedsData[currentFeed] || feedsData.bus_stop;

            const grad = ctx.createLinearGradient(0, 0, canvasElem.width, canvasElem.height);
            grad.addColorStop(0, data.color || '#0f172a');
            grad.addColorStop(1, '#020617');
            ctx.fillStyle = grad;
            ctx.fillRect(0, 0, canvasElem.width, canvasElem.height);

            // Draw grid & decorative HUD lines
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
            ctx.lineWidth = 1;
            for (let x = 0; x < canvasElem.width; x += 40) {
                ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvasElem.height); ctx.stroke();
            }
            for (let y = 0; y < canvasElem.height; y += 40) {
                ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvasElem.width, y); ctx.stroke();
            }

            // Title Banner
            ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
            ctx.font = '600 16px Outfit, sans-serif';
            ctx.fillText(`Sample Feed: ${data.title}`, 20, 36);

            // Render Bounding Boxes on Sample Feed
            if (currentMode === 'assist' || currentMode === 'detect') {
                data.objects.forEach((obj, idx) => {
                    const [rx, ry, rw, rh] = obj.bbox;
                    const x = rx * canvasElem.width;
                    const y = ry * canvasElem.height;
                    const w = rw * canvasElem.width;
                    const h = rh * canvasElem.height;

                    ctx.strokeStyle = idx === 0 ? '#10b981' : '#3b82f6';
                    ctx.lineWidth = 3;
                    ctx.strokeRect(x, y, w, h);

                    ctx.fillStyle = ctx.strokeStyle;
                    ctx.fillRect(x - 2, y - 2, 8, 8);
                    ctx.fillRect(x + w - 6, y - 2, 8, 8);

                    const labelStr = `${obj.label} ${(obj.confidence * 100).toFixed(0)}%`;
                    ctx.font = 'bold 12px JetBrains Mono, monospace';
                    const textWidth = ctx.measureText(labelStr).width;

                    ctx.fillStyle = idx === 0 ? 'rgba(16, 185, 129, 0.9)' : 'rgba(59, 130, 246, 0.9)';
                    ctx.fillRect(x, y - 24 > 0 ? y - 24 : y, textWidth + 12, 24);

                    ctx.fillStyle = '#ffffff';
                    ctx.fillText(labelStr, x + 6, (y - 24 > 0 ? y - 8 : y + 16));
                });
            }

            // Update Perception Logs for Sample Feed
            if (currentMode === 'read') {
                resText.innerText = data.ocrText;
                resObjects.innerHTML = '<span class="tag-empty">YOLO paused in Read mode</span>';
            } else if (currentMode === 'detect') {
                resText.innerText = 'OCR paused in Detect mode';
                resObjects.innerHTML = data.objects.map(o => 
                    `<span class="tag-item">${o.label} (${(o.confidence * 100).toFixed(0)}%)</span>`
                ).join('');
            } else {
                resText.innerText = data.ocrText;
                resObjects.innerHTML = data.objects.map(o => 
                    `<span class="tag-item">${o.label} (${(o.confidence * 100).toFixed(0)}%)</span>`
                ).join('');
            }
        }

        animationFrameId = requestAnimationFrame(renderLoop);
    }

    // --- Announcement Triggers ---
    function triggerAnnounce() {
        if (isWebcamActive) {
            if (currentMode === 'read') {
                speak(liveWebcamOcrText ? `Webcam text reads: ${liveWebcamOcrText}` : 'Reading webcam feed... No clear text detected.');
            } else if (currentMode === 'detect') {
                const names = liveWebcamDetections.map(d => d.class).join(' and ');
                speak(names ? `Webcam detected: ${names}` : 'Scanning webcam feed... Point camera at objects.');
            } else {
                const names = liveWebcamDetections.slice(0, 2).map(d => d.class).join(' and ');
                const textPart = liveWebcamOcrText ? `Text says "${liveWebcamOcrText}".` : '';
                speak(`Webcam vision report: ${textPart} Detected ${names || 'environment items'}.`);
            }
            return;
        }

        const data = feedsData[currentFeed] || feedsData.bus_stop;
        if (currentMode === 'read') {
            speak(`Read text: ${data.ocrText}`);
        } else if (currentMode === 'detect') {
            const objNames = data.objects.map(o => o.label).join(' and ');
            speak(`Detected: ${objNames}`);
        } else {
            speak(data.statement);
        }
    }

    btnManualTrigger.addEventListener('click', triggerAnnounce);
    btnToggleSpeech.addEventListener('click', () => speak(lastSpokenMessage || 'Vision Buddy AI Glass is operational', true));

    btnToggleCamera.addEventListener('click', () => {
        const keys = Object.keys(feedsData);
        const nextIdx = (keys.indexOf(currentFeed) + 1) % keys.length;
        currentFeed = keys[nextIdx];
        sampleFeedSelect.value = currentFeed;
        stopWebcam();
        updateStatementCard();
        speak(`Switched input feed to ${feedsData[currentFeed].title}`);
    });

    toggleThemeBtn.addEventListener('click', () => {
        isMuted = !isMuted;
        muteIcon.className = isMuted ? 'fa-solid fa-volume-xmark' : 'fa-solid fa-volume-high';
        toggleThemeBtn.style.color = isMuted ? '#ef4444' : '#ffffff';
        document.getElementById('ttsPillText').innerText = isMuted ? 'TTS: Muted' : 'TTS: Ready';
        document.getElementById('ttsPill').classList.toggle('active', !isMuted);
        if (!isMuted) speak('Audio unmuted', true);
    });

    // --- Voice Command Execution ---
    commandChips.forEach(chip => {
        chip.addEventListener('click', () => {
            const cmd = chip.getAttribute('data-cmd');
            executeVoiceCommand(cmd);
        });
    });

    micOrb.addEventListener('click', () => {
        speak('Listening for voice command...');
        voicePromptText.innerText = 'Listening...';
        micOrb.style.transform = 'scale(1.1)';

        if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
            const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
            const rec = new SpeechRec();
            rec.lang = 'en-US';
            rec.start();

            rec.onresult = (e) => {
                const phrase = e.results[0][0].transcript;
                executeVoiceCommand(phrase);
            };

            rec.onerror = () => {
                setTimeout(() => executeVoiceCommand('Read this'), 1500);
            };
        } else {
            setTimeout(() => executeVoiceCommand('Read this'), 1200);
        }
    });

    function executeVoiceCommand(cmdText) {
        voiceHeardText.innerText = `"${cmdText}"`;
        voicePromptText.innerText = 'Command recognized!';
        micOrb.style.transform = 'scale(1)';

        const lower = cmdText.toLowerCase();
        if (lower.includes('read')) {
            speak('Command accepted: opening Read Text mode');
            setTimeout(() => navigateTo('camera', 'read'), 1000);
        } else if (lower.includes('detect') || lower.includes('object')) {
            speak('Command accepted: opening Object Detector');
            setTimeout(() => navigateTo('camera', 'detect'), 1000);
        } else if (lower.includes('assist') || lower.includes('start')) {
            speak('Command accepted: starting full AI Assistance');
            setTimeout(() => navigateTo('camera', 'assist'), 1000);
        } else if (lower.includes('sos') || lower.includes('help') || lower.includes('emergency')) {
            speak('Emergency command accepted: launching SOS alert');
            setTimeout(() => navigateTo('sos'), 1000);
        } else {
            speak(`Recognized command ${cmdText}. Executing default scan.`);
            setTimeout(() => navigateTo('camera', 'assist'), 1000);
        }
    }

    // --- SOS Emergency System ---
    btnTriggerSOS.addEventListener('click', () => {
        sosStatusText.innerText = 'ALERT BROADCASTING!';
        sosStatusText.className = 'status-alert';
        sosStatusText.style.color = '#ef4444';
        
        speak('Emergency SOS triggered! Broadcasting location to trusted contacts and sounding siren alarm!', true);

        document.querySelector('.sos-alarm-icon').style.background = '#ef4444';
        document.querySelector('.sos-alarm-icon').style.color = '#ffffff';

        setTimeout(() => {
            alert('🚨 EMERGENCY SOS SENT!\n\nCoordinates: 13.0827° N, 80.2707° E\nSMS Alert dispatched to: Guardian (+91 9876543210)');
        }, 800);
    });

    // Initial greeting
    setTimeout(() => {
        speak('Welcome to Vision Buddy AI Glass simulator. Live webcam AI and sample analytics ready.');
    }, 500);
});
