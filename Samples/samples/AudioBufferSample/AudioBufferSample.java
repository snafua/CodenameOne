package com.codename1.samples;


import com.codename1.capture.Capture;
import com.codename1.components.MultiButton;
import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.io.Util;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.media.AudioBuffer;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.media.WAVWriter;
import com.codename1.ui.FontImage;
import com.codename1.ui.plaf.Style;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class AudioBufferSample {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Capture", BoxLayout.y());
        hi.setToolbar(new Toolbar());
        Style s = UIManager.getInstance().getComponentStyle("Title");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_MIC, s);

        FileSystemStorage fs = FileSystemStorage.getInstance();
        String recordingsDir = fs.getAppHomePath() + "recordings/";
        fs.mkdir(recordingsDir);
        try {
            for (String file : fs.listFiles(recordingsDir)) {
                MultiButton mb = new MultiButton(file.substring(file.lastIndexOf("/") + 1));
                mb.addActionListener((e) -> {
                    try {
                        Media m = MediaManager.createMedia(recordingsDir + file, false);
                        m.play();
                    } catch (Throwable err) {
                        Log.e(err);
                    }
                });
                hi.add(mb);
            }

            hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
                try {
                    String path = "tmpBuffer.pcm";
                    int wavSampleRate = 16000;
                    WAVWriter wavFileWriter = new WAVWriter(new File("tmpBuffer.wav"), wavSampleRate, 1, 16);
                    AudioBuffer audioBuffer = MediaManager.getAudioBuffer(path, true, 64);
                    MediaRecorderBuilder options = new MediaRecorderBuilder()
                            .audioChannels(1)
                            
                            .redirectToAudioBuffer(true)
                           
                            .path(path);
                    System.out.println("Builder isredirect? "+options.isRedirectToAudioBuffer());
                    float[] byteBuffer = new float[audioBuffer.getMaxSize()];
                    audioBuffer.addCallback(buf->{
                        if (buf.getSampleRate() > wavSampleRate) {
                            buf.downSample(wavSampleRate);
                        }
                        buf.copyTo(byteBuffer);
                        
                        try {
                            wavFileWriter.write(byteBuffer, 0, buf.getSize());
                        } catch (Throwable t) {
                            Log.e(t);
                        }

                    });
                                    
                            
                    String file = Capture.captureAudio(options);
                    wavFileWriter.close();
                    SimpleDateFormat sd = new SimpleDateFormat("yyyy-MMM-dd-kk-mm");
                    String fileName = sd.format(new Date());
                    String filePath = recordingsDir + fileName;
                    Util.copy(fs.openInputStream(new File("tmpBuffer.wav").getAbsolutePath()), fs.openOutputStream(filePath));
                    MultiButton mb = new MultiButton(fileName);
                    mb.addActionListener((e) -> {
                        try {
                            Media m = MediaManager.createMedia(filePath, false);
                            m.play();
                        } catch (IOException err) {
                            Log.e(err);
                        }
                    });
                    hi.add(mb);
                    hi.revalidate();
                    if (file != null) {
                        System.out.println(file);
                    }
                } catch (Throwable err) {
                    Log.e(err);
                }
            });
        } catch (Throwable err) {
            Log.e(err);
        }
        hi.show();
    }


    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }

    private void lowLevelUsageSample() {
        // Step 1: Create your audio buffer
        int bufferSize = 256;  // This can be any size you like really.
        String path = "mybuffer.pcm"; // Audio buffer path
                                      // Can be any string, as it doesn't correspond
                                      // to a real file.  It is just used internally
                                      // to identify audio buffers.
        AudioBuffer audioBuffer = MediaManager.getAudioBuffer(path, true, bufferSize);
        
        float[] myFloatBuffer = new float[bufferSize];
            // This float array will be used to copy data out of the audioBuffer
            
        // Step 2: Add callback to audio buffer
        audioBuffer.addCallback(floatSamples->{
            // This callback will be called whenever the contents of the data buffer
            // are changed.
            // This is your "net" to grab the raw PCM samples.
            // floatSamples is a float[] array with the PCM samples. Each sample
            // ranges from -1 to 1.
            
            // IMPORTANT!: This callback is not run on the EDT.  It is called
            // on an internal audio capture thread.
            
            audioBuffer.copyTo(myFloatBuffer);
            
            // All of the new PCM data in in the myFloatBuffer array
            // Do what you like with it - send it to a server, save it to a file,
            // etc...
        });
        
        // Step 3: Create a MediaRecorder
        MediaRecorderBuilder mrb = new MediaRecorderBuilder()
                .path(path)
                .redirectToAudioBuffer(true);
        
        try {
            Media recorder = MediaManager.createMediaRecorder(mrb);
            
            // This actually starts recording.
            recorder.play();
            
            // Record for 5 seconds... use a timer to stop the recorder after that 
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    recorder.cleanup();
                }
                
            }, 5000);
        } catch (IOException ex) {
            Log.p("Failed to create media recorder");
            Log.e(ex);
        }
        
        
    }
    
}
