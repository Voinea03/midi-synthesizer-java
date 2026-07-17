
import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class MidiSynthesizer extends JFrame {

    private static final int START_NOTE = 36;
    private static final int END_NOTE = 108;
    private static final int NUM_CHANNELS = 16;

    private Synthesizer synthesizer;
    private MidiChannel[] midiChannels;
    private Sequencer sequencer;
    private MidiDevice midiInputDevice = null;
    private Transmitter midiInputTransmitter = null;
    private boolean useMidiInput = false;
    private JComboBox<String> midiInputCombo;
    private List<MidiDevice.Info> midiInputDeviceList = new ArrayList<>();
    private int midiInputLowestNote = 48;
    private int midiInputHighestNote = 72;
    private int lpkBaseNote = 48;
    private boolean lpkNeedsCalibration = false;
    private int midiInputKeyCount = 25;
    private int lpkLowestSeen = 48;
    private long lpkResetTime = 0;
    private int recentMinNote = 127;
    private int recentMaxNote = 0;
    private long recentResetTime = 0;

    private Sequence currentSequence;

    private int currentChannel = 0;

    private int manual1Channel = 0;

    private int manual2Channel = 1;

    private int currentManualForChannelSelect = 1;

    private int currentOctaveManual1 = 3;

    private int currentOctaveManual2 = 5;

    private int currentVelocity = 115;

    private int[] channelInstruments = new int[NUM_CHANNELS];
    private int[] channelVolumes = new int[NUM_CHANNELS];
    private int[] channelPans = new int[NUM_CHANNELS];
    private int[] channelReverb = new int[NUM_CHANNELS];
    private int[] channelChorus = new int[NUM_CHANNELS];
    private int[] channelModulation = new int[NUM_CHANNELS];
    private int[] channelBrightness = new int[NUM_CHANNELS];
    private int[] channelResonance = new int[NUM_CHANNELS];
    private int[] channelAttack = new int[NUM_CHANNELS];
    private int[] channelRelease = new int[NUM_CHANNELS];
    private int[] channelExpression = new int[NUM_CHANNELS];
    private int[] channelVibratoRate = new int[NUM_CHANNELS];
    private int[] channelVibratoDepth = new int[NUM_CHANNELS];
    private int[] channelDetune = new int[NUM_CHANNELS];
    private boolean[] channelMute = new boolean[NUM_CHANNELS];
    private boolean[] channelSolo = new boolean[NUM_CHANNELS];
    private int[] channelActivity = new int[NUM_CHANNELS];

    private Set<Integer> activeNotes = new HashSet<>();
    private Set<Integer> pressedKeys = new HashSet<>( );

    private boolean isPlaying = false;

    private boolean isPaused = false;

    private long pausePosition = 0;

    private boolean isRecording = false;

    private long recordingStartTime = 0;

    private Sequence recordedSequence;
    private Track recordingTrack;

    private int recordingResolution = 480;

    private int recordingBPM = 120;

    private boolean metronomeEnabled = false;
    private Timer metronomeTimer;

    private Deque<Sequence> historyStack = new ArrayDeque<>();

    private Sequence originalSequence = null;
    private static final int MAX_HISTORY = 20;

    private List<VisualNote> visualNotes = Collections.synchronizedList(new ArrayList<>());

    private PianoPanel pianoPanel;
    private NoteVisualizerPanel vizPanel;
    private MixerPanel mixerPanel;
    private JComboBox<String> instrumentCombo;
    private JComboBox<String> categoryCombo;
    private static final String[] CATEGORIES = {
        "Piano", "Chromatic Perc", "Organ", "Guitar",
        "Bass", "Strings", "Ensemble", "Brass",
        "Reed", "Pipe", "Synth Lead", "Synth Pad",
        "Synth Effects", "Ethnic", "Percussive", "Sound FX"
    };
    private JSlider volumeSlider,panSlider, tempoSlider, reverbSlider, chorusSlider, velocitySlider;
    private JSlider pitchBendSlider;

    private JLabel statusLabel, channelLabel, noteDisplayLabel, chordDisplayLabel, octaveLabel, beatCountLabel;
    private JLabel m1Label, m2Label;
    private JLabel m1ChLabel,m2ChLabel;

    private JButton playBtn,pauseBtn, stopBtn, loadBtn, recordBtn;
    private JProgressBar progressBar;
    private Timer progressTimer,vuTimer;
    private JTextArea transformLogArea;

    private JPanel beatIndicatorPanel;

    private int beatCount = 0;
    private int[] clipboardSettings = null;
    private int clipboardSourceChannel = -1;
    private int[] channelActiveEffect = new int[NUM_CHANNELS];
    private JButton[] effectButtons;
    private JLabel activeEffectLabel;

    private final Map<Integer, Integer> manual1Map = new LinkedHashMap<>();
    private final Map<Integer, Integer> manual2Map = new LinkedHashMap<>();

    private static final String[] GM_INSTRUMENTS = {
        "Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano",
        "Honky-tonk Piano", "Electric Piano 1", "Electric Piano 2", "Harpsichord",
        "Clavinet", "Celesta", "Glockenspiel", "Music Box", "Vibraphone",
        "Marimba", "Xylophone", "Tubular Bells", "Dulcimer", "Drawbar Organ",
        "Percussive Organ", "Rock Organ", "Church Organ", "Reed Organ",
        "Accordion", "Harmonica", "Tango Accordion", "Nylon Guitar",
        "Steel Guitar", "Jazz Guitar", "Clean Guitar", "Muted Guitar",
        "Overdriven Guitar", "Distortion Guitar", "Guitar Harmonics",
        "Acoustic Bass", "Finger Bass", "Pick Bass", "Fretless Bass",
        "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2",
        "Violin", "Viola", "Cello", "Contrabass", "Tremolo Strings",
        "Pizzicato Strings", "Orchestral Harp", "Timpani", "String Ensemble 1",
        "String Ensemble 2", "Synth Strings 1", "Synth Strings 2", "Choir Aahs",
        "Voice Oohs", "Synth Voice", "Orchestra Hit", "Trumpet", "Trombone",
        "Tuba", "Muted Trumpet", "French Horn", "Brass Section", "Synth Brass 1",
        "Synth Brass 2", "Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax",
        "Oboe", "English Horn", "Bassoon", "Clarinet", "Piccolo", "Flute",
        "Recorder", "Pan Flute", "Blown Bottle", "Shakuhachi", "Whistle",
        "Ocarina", "Square Lead", "Sawtooth Lead", "Calliope Lead", "Chiff Lead",
        "Charang Lead", "Voice Lead", "Fifths Lead", "Bass+Lead", "New Age Pad",
        "Warm Pad", "Polysynth Pad", "Choir Pad", "Bowed Pad", "Metallic Pad",
        "Halo Pad", "Sweep Pad", "Rain FX", "Soundtrack FX", "Crystal FX",
        "Atmosphere FX", "Brightness FX", "Goblins FX", "Echoes FX", "Sci-Fi FX",
        "Sitar", "Banjo", "Shamisen", "Koto", "Kalimba", "Bagpipe", "Fiddle",
        "Shanai", "Tinkle Bell", "Agogo", "Steel Drums", "Woodblock",
        "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal",
        "Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet",
        "Telephone Ring", "Helicopter", "Applause", "Gunshot"
    };
    private static final String[] NOTE_NAMES = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
    private static final String[] SOLFEGE_NAMES = {"Do","Do#","Re","Re#","Mi","Fa","Fa#","Sol","Sol#","La","La#","Si"};

    private static final Color[] NOTE_COLORS = {
        new Color(220, 70, 70),
        new Color(220, 110, 60),
        new Color(230, 160, 50),
        new Color( 230, 200, 60),
        new Color(210, 220, 70),
        new Color(120, 200, 80),
        new Color(70, 200, 140),
        new Color(60, 180, 200),
        new Color(70, 130, 220),
        new Color(120, 90, 220),
        new Color(180, 80, 220),
        new Color(220, 80, 170)
    };

    private static final Color BG_DARK = new Color(22, 22, 28);
    private static final Color BG_PANEL = new Color(30, 30, 38);
    private static final Color BG_ELEMENT = new Color(40, 40, 50);
    private static final Color BG_HOVER = new Color(50, 50, 62);
    private static final Color ACCENT = new Color(70, 130, 200);
    private static final Color ACCENT_GREEN = new Color( 50, 180, 100);
    private static final Color ACCENT_RED = new Color(220, 60, 60);
    private static final Color ACCENT_YELLOW = new Color(220, 180, 50);
    private static final Color TEXT_PRIMARY = new Color(220, 220, 225);
    private static final Color TEXT_DIM = new Color(130, 130, 140);
    private static final Color BORDER_COLOR = new Color(55, 55, 65);

    private static final Color[] CH_COLORS = {
        new Color(100,180,255), new Color(255,100,100), new Color(100,230,130),
        new Color(255,200,80),  new Color(190,140,255), new Color(255,140,190),
        new Color(80,230,210),  new Color(240,240,120), new Color(160,160,255),
        new Color(200,120,50),  new Color(130,240,130), new Color(255,170,170),
        new Color(170,210,255), new Color(210,170,255), new Color(255,210,170),
        new Color(170,245,210)
    };

    public MidiSynthesizer() {
        super("MIDI Synthesizer Pro");
        initKeyMappings( );
        initMidi();
        initUI();
        initKeyboardListener();
        startVUTimer();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1200, 750));
        setLocationRelativeTo(null);
        setVisible( true);
        log("MIDI Synthesizer Pro initializat.");
    }

    private void initKeyMappings() {
        manual1Map.put( KeyEvent.VK_Z,0);
        manual1Map.put(KeyEvent.VK_S,1);
        manual1Map.put(KeyEvent.VK_X,2);
        manual1Map.put( KeyEvent.VK_D,3);
        manual1Map.put(KeyEvent.VK_C,4);
        manual1Map.put(KeyEvent.VK_V,5);
        manual1Map.put(KeyEvent.VK_G,6);
        manual1Map.put(KeyEvent.VK_B,7);
        manual1Map.put(KeyEvent.VK_H,8);
        manual1Map.put(KeyEvent.VK_N,9);
        manual1Map.put(KeyEvent.VK_J,10);
        manual1Map.put(KeyEvent.VK_M,11);
        manual1Map.put(KeyEvent.VK_COMMA,12);
        manual2Map.put(KeyEvent.VK_Q,0);
        manual2Map.put(KeyEvent.VK_2,1);
        manual2Map.put( KeyEvent.VK_W,2);
        manual2Map.put( KeyEvent.VK_3,3);
        manual2Map.put(KeyEvent.VK_E,4);
        manual2Map.put(KeyEvent.VK_R,5);
        manual2Map.put(KeyEvent.VK_5,6);
        manual2Map.put(KeyEvent.VK_T,7);
        manual2Map.put(KeyEvent.VK_6,8);
        manual2Map.put(KeyEvent.VK_Y,9);
        manual2Map.put(KeyEvent.VK_7,10);
        manual2Map.put(KeyEvent.VK_U,11);
        manual2Map.put(KeyEvent.VK_I,12);
    }

    private void refreshMidiInputCombo() {
        scanMidiInputs();
        midiInputCombo.removeAllItems();
        midiInputCombo.addItem("— Tastatura PC —");
        for (MidiDevice.Info info : midiInputDeviceList) {
            midiInputCombo.addItem( info.getName());
        }
    }

    private void scanMidiInputs() {
        midiInputDeviceList.clear();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice dev = MidiSystem.getMidiDevice(info);
                if (dev.getMaxTransmitters() != 0
                    && !(dev instanceof Sequencer)
                    && !(dev instanceof Synthesizer)) {
                    midiInputDeviceList.add(info);
                }
            } catch (MidiUnavailableException ignored) {}
        }
    }

    private void connectMidiInput(MidiDevice.Info info) {
        disconnectMidiInput();
        if (info == null) return;
        lpkNeedsCalibration = true;
        try {
            midiInputDevice = MidiSystem.getMidiDevice(info);
            if( !midiInputDevice.isOpen()) midiInputDevice.open();
            midiInputTransmitter = midiInputDevice.getTransmitter( );
            midiInputTransmitter.setReceiver(new Receiver() {
                @Override
                public void send(MidiMessage msg,long ts) {
                    if (!useMidiInput) return;

                    if (msg instanceof ShortMessage) {
                        ShortMessage smDbg = (ShortMessage) msg;
                        int c = smDbg.getCommand();
                        if(c != ShortMessage.NOTE_ON && c != ShortMessage.NOTE_OFF) {
                            String typeStr;
                            switch (c) {
                                case ShortMessage.CONTROL_CHANGE: typeStr = "CC"; break;
                                case ShortMessage.PROGRAM_CHANGE: typeStr = "PROGRAM"; break;
                                case ShortMessage.PITCH_BEND: typeStr = "PITCH_BEND"; break;
                                case ShortMessage.CHANNEL_PRESSURE: typeStr = "AFTERTOUCH"; break;
                                case ShortMessage.POLY_PRESSURE: typeStr = "POLY"; break;
                                default: typeStr = "0x" + Integer.toHexString(c);
                            }
                            int d1 = smDbg.getData1();
                            int d2 = smDbg.getData2();
                            SwingUtilities.invokeLater(() ->
                                log("[MIDI IN] " + typeStr + " ch=" + smDbg.getChannel() + " d1=" + d1 + " d2=" + d2));
                        }
                    } else if (msg instanceof SysexMessage) {
                        SysexMessage sx = (SysexMessage) msg;
                        byte[] data = sx.getData();
                        StringBuilder hex = new StringBuilder();
                        for (int i = 0; i < Math.min(data.length, 16); i++) {
                            hex.append(String.format("%02X ", data[i] & 0xFF));
                        }
                        SwingUtilities.invokeLater(() ->
                            log("[MIDI IN] SYSEX len=" + data.length + " " + hex));
                    }

                    if(!(msg instanceof ShortMessage)) return;
                    ShortMessage sm = (ShortMessage) msg;
                    int cmd = sm.getCommand();
                    int note = sm.getData1();
                    int vel = sm.getData2();

                    SwingUtilities.invokeLater(() -> {
                        if(lpkNeedsCalibration && cmd == ShortMessage.NOTE_ON && vel > 0) {
                            int startC = (note / 12) * 12;
                            lpkBaseNote = startC;
                            midiInputLowestNote = startC;
                            midiInputHighestNote = Math.min(END_NOTE, startC + midiInputKeyCount - 1);
                            lpkNeedsCalibration = false;
                            if (pianoPanel != null) pianoPanel.repaint();
                            if(learnWindow != null && learnWindow.isDisplayable()) learnWindow.repaint();
                            updateOctaveDisplay();
                        }

                        int offset = midiInputLowestNote - lpkBaseNote;
                        int adjustedNote = note + offset;
                        while (adjustedNote < START_NOTE) adjustedNote += 12;
                        while (adjustedNote > END_NOTE) adjustedNote -= 12;
                        final int finalNote = adjustedNote;

                        if (cmd == ShortMessage.NOTE_ON && vel > 0) {
                            updateMidiInputRange(finalNote);

                            int targetCh = currentChannel;
                            if(midiChannels[targetCh] != null) {
                                midiChannels[targetCh].noteOn(finalNote, vel);
                            }
                            activeNotes.add(finalNote);
                            channelActivity[targetCh] = Math.min( 127, vel + 20);
                            visualNotes.add(new VisualNote(finalNote, System.currentTimeMillis(),
                                targetCh, CH_COLORS[targetCh]));
                            pianoPanel.repaint();
                            updateNoteDisplay();

                            if(learnWindow != null && learnWindow.isDisplayable()) {
                                learnWindow.externalNoteOn(finalNote, vel);
                            }

                            if(isRecording && recordingTrack != null) {
                                try {
                                    long tick = millisToTick( System.currentTimeMillis() - recordingStartTime);
                                    ShortMessage m = new ShortMessage();
                                    m.setMessage(ShortMessage.NOTE_ON, targetCh, finalNote, vel);
                                    recordingTrack.add(new MidiEvent(m, tick));
                                } catch (InvalidMidiDataException ignored) {}
                            }
                        } else if (cmd == ShortMessage.NOTE_OFF
                                || (cmd == ShortMessage.NOTE_ON && vel == 0)) {
                            int targetCh = currentChannel;
                            if (midiChannels[targetCh] != null) {
                                midiChannels[targetCh].noteOff(finalNote);
                            }
                            activeNotes.remove(finalNote);
                            pianoPanel.repaint();
                            updateNoteDisplay();
                            synchronized (visualNotes) {
                                for(int i = visualNotes.size() - 1; i >= 0; i--) {
                                    VisualNote vn = visualNotes.get(i);
                                    if(vn.note == finalNote && vn.channel == targetCh && vn.duration < 0) {
                                        vn.duration = System.currentTimeMillis() - vn.startTime;
                                        break;
                                    }
                                }
                            }

                            if (learnWindow != null && learnWindow.isDisplayable()) {
                                learnWindow.externalNoteOff(finalNote);
                            }

                            if (isRecording && recordingTrack != null) {
                                try {
                                    long tick = millisToTick(System.currentTimeMillis() - recordingStartTime);
                                    ShortMessage m = new ShortMessage( );
                                    m.setMessage(ShortMessage.NOTE_OFF, targetCh, finalNote, 0);
                                    recordingTrack.add(new MidiEvent(m, tick));
                                } catch (InvalidMidiDataException ignored) {}
                            }
                        }
                    });
                }
                @Override public void close() {}
            });
            log("Conectat MIDI input: " + info.getName());
        } catch (MidiUnavailableException e) {
            log("Eroare conectare MIDI input: " + e.getMessage());
        }
    }

    private void disconnectMidiInput() {
        if(midiInputTransmitter != null) {
            try { midiInputTransmitter.close(); } catch (Exception ignored) {}
            midiInputTransmitter = null;
        }
        if(midiInputDevice != null && midiInputDevice.isOpen()) {
            try { midiInputDevice.close(); } catch (Exception ignored) {}
            midiInputDevice = null;
        }
    }

    private void updateMidiInputRange(int note) {
    }

    private void shiftMidiInputRange(int semitones) {
        releaseAllNotes();
        int newStart = midiInputLowestNote + semitones;
        if(newStart < START_NOTE) newStart = START_NOTE;
        int maxStart = END_NOTE - (midiInputKeyCount - 1);
        if( newStart > maxStart) newStart = maxStart;
        midiInputLowestNote = newStart;
        midiInputHighestNote = newStart + midiInputKeyCount - 1;
        if (pianoPanel != null) pianoPanel.repaint();
        if(learnWindow != null && learnWindow.isDisplayable()) {
            learnWindow.repaint();
        }
        updateOctaveDisplay();
    }

    private void initMidi() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            midiChannels = synthesizer.getChannels();
            sequencer = MidiSystem.getSequencer( false);
            sequencer.open();
            for (int i = 0; i < NUM_CHANNELS; i++) {
                channelInstruments[i]=0;
                channelVolumes[i]=120;
                channelPans[i]=64;
                channelReverb[i]=40;
                channelChorus[i]=0;
                channelModulation[i]=0;
                channelBrightness[i]=64;
                channelResonance[i]=64;
                channelAttack[i]=64;
                channelRelease[i]=64;
                channelExpression[i]=127;
                channelVibratoRate[i]=64;
                channelVibratoDepth[i]=0;
                channelDetune[i]=64;
                if (midiChannels[i]!=null) { midiChannels[i].controlChange(7,120); midiChannels[i].controlChange(10,64);
                    midiChannels[i].controlChange(91,40);
                    midiChannels[i].controlChange(93,0);
                    midiChannels[i].controlChange( 1,0);
                    midiChannels[i].controlChange(11,127);
                    midiChannels[i].controlChange(74,64);
                    midiChannels[i].controlChange(71,64);
                    midiChannels[i].controlChange(73,64);
                    midiChannels[i].controlChange( 72,64);
                    }
            }
            sequencer.addMetaEventListener(m -> {
                if(m.getType() == 47) {
                    SwingUtilities.invokeLater(() -> {
                        isPlaying = false;
                        isPaused = false;
                        updatePlaybackButtons();
                        log("Redare completa.");
                    });
                }
            });

            Transmitter seqTransmitter = sequencer.getTransmitter();
            Receiver synthReceiver = synthesizer.getReceiver();
            seqTransmitter.setReceiver(new Receiver() {

                @Override public void send(MidiMessage msg, long timeStamp) {

                    synthReceiver.send(msg, timeStamp);

                    if(msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        int ch = sm.getChannel( );
                        if( sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                            channelActivity[ch] = Math.min( 127,sm.getData2() + 20);
                            visualNotes.add(new VisualNote(sm.getData1(), System.currentTimeMillis(), ch, CH_COLORS[ch]));
                        } else if (sm.getCommand() == ShortMessage.NOTE_OFF ||
                                   (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                            synchronized(visualNotes) {
                                for(int i = visualNotes.size() - 1; i >= 0; i--) {
                                    VisualNote vn = visualNotes.get(i);
                                    if(vn.note == sm.getData1() && vn.channel == ch && vn.duration < 0) {
                                        vn.duration = System.currentTimeMillis() - vn.startTime;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                @Override public void close() {}
            });
        } catch (MidiUnavailableException e) { JOptionPane.showMessageDialog(this,"Eroare MIDI: "+e.getMessage(),"Eroare",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(0,0));
        getContentPane().setBackground(BG_DARK);
        add(createTopBar(), BorderLayout.NORTH);
        JPanel mainBody = new JPanel(new BorderLayout(0,0));
        mainBody.setBackground(BG_DARK);
        JPanel centerStack = new JPanel(new BorderLayout(0,2));
        centerStack.setBackground(BG_DARK);

        vizPanel = new NoteVisualizerPanel();
        mixerPanel = new MixerPanel();
        mixerPanel.setPreferredSize(new Dimension(0, 280));
        centerStack.add( vizPanel, BorderLayout.CENTER);
        centerStack.add(mixerPanel, BorderLayout.SOUTH);
        mainBody.add(centerStack, BorderLayout.CENTER);
        mainBody.add(createRightPanel(), BorderLayout.EAST);
        add(mainBody, BorderLayout.CENTER);
        JPanel bottomStack = new JPanel(new BorderLayout(0,0));
        bottomStack.setBackground(BG_DARK);
        bottomStack.add(createPianoContainer(), BorderLayout.CENTER);
        add(bottomStack, BorderLayout.SOUTH);
    }

    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout(8,0));
        top.setBackground(new Color(26,26,34));
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        JPanel transport = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        transport.setOpaque( false);
        loadBtn=mkBtn("LOAD",ACCENT,11);
        recordBtn=mkBtn("REC",ACCENT_RED,11);
        playBtn=mkBtn("PLAY",ACCENT_GREEN,11);
        pauseBtn=mkBtn("PAUSE",ACCENT_YELLOW,11);
        stopBtn=mkBtn("STOP",new Color(140,60,60),11);
        JButton learnBtn = mkBtn("INVATA", new Color(150, 80, 180), 11);
        loadBtn.addActionListener(e->loadMidiFile());
        recordBtn.addActionListener(e->toggleRecording());
        playBtn.addActionListener(e->playMidi());
        pauseBtn.addActionListener(e->pauseMidi());
        stopBtn.addActionListener(e->stopMidi());
        learnBtn.addActionListener(e->openLearnWindow());
        transport.add(loadBtn);
        transport.add(Box.createHorizontalStrut(6));
        transport.add( recordBtn);
        transport.add(Box.createHorizontalStrut(6));
        transport.add(playBtn);
        transport.add(pauseBtn);
        transport.add(stopBtn);
        transport.add(Box.createHorizontalStrut(10));
        transport.add(learnBtn);
        transport.add(Box.createHorizontalStrut(6));
        JButton helpBtn = mkBtn("?", new Color(100, 130, 160), 12);
        helpBtn.setToolTipText("Afiseaza scurtaturile de tastatura");
        helpBtn.setPreferredSize(new Dimension(32, 24));
        helpBtn.addActionListener(e -> showShortcutsHelp());
        transport.add(helpBtn);
        transport.add(Box.createHorizontalStrut(10));

        JPanel midiInPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        midiInPanel.setOpaque(false);
        JLabel midiInLabel = new JLabel("Input:");
        midiInLabel.setForeground(TEXT_DIM);
        midiInLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        midiInputCombo = new JComboBox<>();
        midiInputCombo.setFocusable(false);
        midiInputCombo.setFont(new Font("SansSerif", Font.PLAIN, 10));
        midiInputCombo.setPreferredSize(new Dimension(180, 24));
        refreshMidiInputCombo();
        midiInputCombo.addActionListener(e -> {
            int idx = midiInputCombo.getSelectedIndex();
            if(idx <= 0) {
                useMidiInput = false;
                disconnectMidiInput();
                log("Folosesc tastatura PC");
            } else {
                useMidiInput = true;
                MidiDevice.Info info = midiInputDeviceList.get(idx - 1);
                connectMidiInput( info);
            }
            if (pianoPanel != null) pianoPanel.repaint();
            if (learnWindow != null && learnWindow.isDisplayable()) learnWindow.repaint();
            updateOctaveDisplay();
            updateManualChannelLabels();
        });
        midiInPanel.add(midiInLabel);
        midiInPanel.add(midiInputCombo);
        transport.add(midiInPanel);
        transport.add(Box.createHorizontalStrut(10));
        beatIndicatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,3,4));
        beatIndicatorPanel.setOpaque(false);
        for (int i=0; i<4; i++) { JPanel dot = new JPanel(){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(getBackground()); g2.fillOval(0,0,getWidth(),getHeight());}};
            dot.setPreferredSize(new Dimension(10,10));
            dot.setOpaque(false);
            dot.setBackground( new Color(50,50,60));
            beatIndicatorPanel.add(dot);
            }
        transport.add(beatIndicatorPanel);
        JPanel center = new JPanel(new BorderLayout(8,0));
        center.setOpaque(false);
        progressBar = new JProgressBar( 0,100);
        progressBar.setStringPainted(true);
        progressBar.setString("--:-- / --:--");
        progressBar.setBackground(BG_ELEMENT);
        progressBar.setForeground(ACCENT);
        progressBar.setFont(new Font("Monospaced",Font.PLAIN,11));
        progressBar.setPreferredSize(new Dimension(250,22));
        progressBar.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        JPanel tempoP = new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));
        tempoP.setOpaque(false);
        JLabel tL = new JLabel("BPM");
        tL.setForeground(TEXT_DIM);
        tL.setFont(new Font("SansSerif",Font.PLAIN,10));
        tempoSlider = new JSlider(20,300,120);
        tempoSlider.setOpaque(false);
        tempoSlider.setPreferredSize(new Dimension(120,20));
        tempoSlider.setFocusable(false);
        JLabel tV = new JLabel("120");
        tV.setForeground(TEXT_PRIMARY);
        tV.setFont(new Font("Monospaced",Font.BOLD,12));
        tV.setPreferredSize(new Dimension(30,20));
        tempoSlider.addChangeListener(e -> {
                tV.setText("" + tempoSlider.getValue());
                if (sequencer != null && sequencer.isOpen()) {
                    sequencer.setTempoInBPM(tempoSlider.getValue());
                }
            });
        JCheckBox metroCheck = new JCheckBox("Beat");
        metroCheck.setOpaque(false);
        metroCheck.setForeground(TEXT_DIM);
        metroCheck.setFont(new Font("SansSerif",Font.PLAIN,10));
        metroCheck.setFocusable(false);
        metroCheck.addActionListener(e->metronomeEnabled=metroCheck.isSelected());
        tempoP.add(tL);
        tempoP.add(tempoSlider);
        tempoP.add(tV);
        tempoP.add(metroCheck);
        center.add(progressBar, BorderLayout.CENTER);
        center.add(tempoP, BorderLayout.EAST);
        JPanel rightInfo = new JPanel(new GridLayout(1, 3, 8, 0));
        rightInfo.setOpaque(false);
        rightInfo.setPreferredSize(new Dimension(340, 0));
        noteDisplayLabel = new JLabel("---", SwingConstants.CENTER);
        noteDisplayLabel.setForeground(ACCENT);
        noteDisplayLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        statusLabel = new JLabel("Pregatit", SwingConstants.CENTER);
        statusLabel.setForeground(ACCENT_GREEN);
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        beatCountLabel = new JLabel("Beat: 0", SwingConstants.CENTER);
        beatCountLabel.setForeground(ACCENT_YELLOW);
        beatCountLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        rightInfo.add(statusLabel);
        rightInfo.add(noteDisplayLabel);
        rightInfo.add(beatCountLabel);
        top.add(transport, BorderLayout.WEST);
        top.add(center, BorderLayout.CENTER);
        top.add(rightInfo, BorderLayout.EAST);
        return top;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(0,0));
        right.setBackground(BG_PANEL);
        right.setPreferredSize(new Dimension(270,0));
        right.setBorder(BorderFactory.createMatteBorder(0,1,0,0,BORDER_COLOR));
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setBackground(BG_PANEL);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setFont(new Font("SansSerif",Font.BOLD,11));
        tabs.setFocusable(false);
        tabs.addTab("Canal", createChannelTab());
        tabs.addTab("Efecte", createEffectsTab());
        tabs.addTab("Transformari", createTransformTab());
        right.add(tabs, BorderLayout.CENTER);
        return right;
    }

    private JPanel createChannelTab() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground( BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));
        channelLabel = new JLabel("Canal 1 - Piano");
        channelLabel.setForeground(TEXT_PRIMARY);
        channelLabel.setFont(new Font("SansSerif",Font.BOLD,14));
        channelLabel.setAlignmentX( Component.LEFT_ALIGNMENT);
        p.add( channelLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(mkLabel("Categorie"));
        categoryCombo = new JComboBox<>(CATEGORIES);
        categoryCombo.setMaximumSize(new Dimension(250, 24));
        categoryCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryCombo.setFocusable(false);
        categoryCombo.setFont(new Font("SansSerif", Font.BOLD, 11));
        categoryCombo.addActionListener( e -> {
            int cat = categoryCombo.getSelectedIndex();
            instrumentCombo.removeAllItems();
            for (int i = cat * 8; i < cat * 8 + 8; i++) {
                instrumentCombo.addItem(GM_INSTRUMENTS[i]);
            }
        });
        p.add( categoryCombo);
        p.add(Box.createVerticalStrut(4));

        p.add(mkLabel("Instrument"));
        instrumentCombo = new JComboBox<>();
        for (int i = 0; i < 8; i++) instrumentCombo.addItem(GM_INSTRUMENTS[i]);
        instrumentCombo.setMaximumSize(new Dimension(250, 24));
        instrumentCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        instrumentCombo.setFocusable(false);
        instrumentCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        instrumentCombo.addActionListener(e -> {
            if (instrumentCombo.getSelectedIndex() < 0) return;
            int prog = categoryCombo.getSelectedIndex() * 8 + instrumentCombo.getSelectedIndex();
            channelInstruments[currentChannel] = prog;
            if(midiChannels[currentChannel] != null) midiChannels[currentChannel].programChange(prog);
            channelLabel.setText("Canal " + (currentChannel + 1) + " - " + GM_INSTRUMENTS[prog]);
        });
        p.add(instrumentCombo);
        p.add(Box.createVerticalStrut(10));
        p.add(mkSlider("Volum", 0, 127, 120, s -> {
                volumeSlider = s;
                s.addChangeListener(e -> {
                    channelVolumes[currentChannel] = s.getValue();
                    if(midiChannels[currentChannel] != null)
                        midiChannels[currentChannel].controlChange(7, s.getValue());
                });
            }));
        p.add(mkSlider("Pan L-R", 0, 127, 64, s -> {
                panSlider = s;
                s.addChangeListener(e -> {
                    channelPans[currentChannel] = s.getValue();
                    if(midiChannels[currentChannel] != null)
                        midiChannels[currentChannel].controlChange( 10, s.getValue());
                });
            }));
        p.add(mkSlider("Reverb", 0, 127, 40, s -> {
                reverbSlider = s;
                s.addChangeListener(e -> {
                    channelReverb[currentChannel] = s.getValue();
                    if(midiChannels[currentChannel] != null)
                        midiChannels[currentChannel].controlChange(91, s.getValue());
                });
            }));
        p.add(mkSlider("Chorus", 0, 127, 0, s -> {
                chorusSlider = s;
                s.addChangeListener(e -> {
                    channelChorus[currentChannel] = s.getValue( );
                    if (midiChannels[currentChannel] != null)
                        midiChannels[currentChannel].controlChange(93,s.getValue());
                });
            }));
        p.add(mkSlider("Velocity", 1, 127, 115, s -> {
                velocitySlider = s;
                s.addChangeListener( e -> currentVelocity = s.getValue());
            }));
        p.add(Box.createVerticalStrut(6));
        p.add(mkLabel("Pitch Bend"));
        pitchBendSlider = new JSlider(-8192,8191,0);
        pitchBendSlider.setOpaque(false);
        pitchBendSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        pitchBendSlider.setMaximumSize(new Dimension(250,22));
        pitchBendSlider.setFocusable(false);
        pitchBendSlider.addChangeListener(e -> {
                if (midiChannels[currentChannel] != null) {
                    midiChannels[currentChannel].setPitchBend(pitchBendSlider.getValue() + 8192);
                }
            });
        pitchBendSlider.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    pitchBendSlider.setValue( 0);
                }
            });
        p.add(pitchBendSlider);
        p.add(Box.createVerticalStrut(8));
        chordDisplayLabel = new JLabel("");
        chordDisplayLabel.setVisible(false);
        p.add(Box.createVerticalStrut(4));
        JPanel octRow = new JPanel( );
        octRow.setOpaque(false);
        octRow.setLayout(new BoxLayout(octRow, BoxLayout.X_AXIS));
        octRow.setAlignmentX( Component.LEFT_ALIGNMENT);
        m1Label = new JLabel("M1: octava " + currentOctaveManual1);
        m1Label.setForeground(ACCENT);
        m1Label.setFont(new Font("Monospaced", Font.BOLD, 11));
        m2Label = new JLabel("M2: octava " + currentOctaveManual2);
        m2Label.setForeground(ACCENT);
        m2Label.setFont(new Font("Monospaced", Font.BOLD, 11));
        octRow.add(m1Label);
        octRow.add(Box.createHorizontalStrut(12));
        octRow.add(m2Label);
        p.add(octRow);
        octaveLabel = m1Label;
        p.add(Box.createVerticalStrut(8));

        p.add(mkLabel("Canal per Manual"));
        JPanel manualChPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        manualChPanel.setOpaque(false);
        manualChPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        manualChPanel.setMaximumSize(new Dimension(250, 50));

        m1ChLabel = new JLabel("M1: Canal 1");
        m1ChLabel.setForeground(new Color(100, 180, 255));
        m1ChLabel.setFont(new Font("Monospaced", Font.BOLD, 11));

        m2ChLabel = new JLabel("M2: Canal 1");
        m2ChLabel.setForeground( new Color(255, 180, 100));
        m2ChLabel.setFont(new Font("Monospaced", Font.BOLD, 11));

        manualChPanel.add(m1ChLabel);
        manualChPanel.add(m2ChLabel);
        p.add( manualChPanel);

        p.add(Box.createVerticalStrut(8));
        p.add(mkLabel("Copy / Paste Canal"));
        JLabel clipLabel = new JLabel("Clipboard: gol");
        clipLabel.setForeground(TEXT_DIM);
        clipLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        clipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cpPanel.setOpaque(false);
        cpPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cpPanel.setMaximumSize(new Dimension(250, 28));

        JButton copyBtn = mkBtn("Copy", new Color(80, 100, 80), 10);
        copyBtn.addActionListener(e -> {
            clipboardSettings = new int[]{
                channelInstruments[currentChannel],channelVolumes[currentChannel],
                channelPans[currentChannel], channelReverb[currentChannel],
                channelChorus[currentChannel],channelModulation[currentChannel],
                channelBrightness[currentChannel], channelResonance[currentChannel],
                channelAttack[currentChannel], channelRelease[currentChannel],
                channelExpression[currentChannel]
            };
            clipboardSourceChannel = currentChannel;
            clipLabel.setText("Clipboard: Ch " + (currentChannel + 1) + " (" +
                GM_INSTRUMENTS[channelInstruments[currentChannel]] + ")");
            clipLabel.setForeground(ACCENT_GREEN);
            log("Copiat setarile canalului " + (currentChannel + 1));
        });

        JButton pasteBtn = mkBtn("Paste", new Color(100, 80, 80), 10);
        pasteBtn.addActionListener(e -> {
            if (clipboardSettings == null) {
                log("Clipboard gol. Apasati Copy mai intai.");
                return;
            }
            channelInstruments[currentChannel] = clipboardSettings[0];
            channelVolumes[currentChannel] = clipboardSettings[1];
            channelPans[currentChannel] = clipboardSettings[2];
            channelReverb[currentChannel] = clipboardSettings[3];
            channelChorus[currentChannel] = clipboardSettings[4];
            channelModulation[currentChannel] = clipboardSettings[5];
            channelBrightness[currentChannel] = clipboardSettings[6];
            channelResonance[currentChannel] = clipboardSettings[7];
            channelAttack[currentChannel] = clipboardSettings[8];
            channelRelease[currentChannel] = clipboardSettings[9];
            channelExpression[currentChannel] = clipboardSettings[10];

            if (midiChannels[currentChannel] != null) {
                midiChannels[currentChannel].programChange(clipboardSettings[0]);
                midiChannels[currentChannel].controlChange(7, clipboardSettings[1]);
                midiChannels[currentChannel].controlChange(10, clipboardSettings[2]);
                midiChannels[currentChannel].controlChange(91, clipboardSettings[3]);
                midiChannels[currentChannel].controlChange(93, clipboardSettings[4]);
                midiChannels[currentChannel].controlChange(1, clipboardSettings[5]);
                midiChannels[currentChannel].controlChange(74, clipboardSettings[6]);
                midiChannels[currentChannel].controlChange(71, clipboardSettings[7]);
                midiChannels[currentChannel].controlChange(73,clipboardSettings[8]);
                midiChannels[currentChannel].controlChange(72, clipboardSettings[9]);
                midiChannels[currentChannel].controlChange(11, clipboardSettings[10]);
            }

            selectChannel(currentChannel);

            for(Component c : mixerPanel.getComponents()) {
                if(c instanceof ChannelStrip && ((ChannelStrip) c).ch == currentChannel) {
                    ((ChannelStrip) c).volSlider.setValue(clipboardSettings[1]);
                }
            }

            log("Lipit setarile de pe Ch " + (clipboardSourceChannel + 1) +
                " pe Ch " + (currentChannel + 1));
        });

        cpPanel.add(copyBtn);
        cpPanel.add(pasteBtn);
        p.add(cpPanel);
        p.add(clipLabel);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createEffectsTab() {
        JPanel p = new JPanel( );
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground( BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));

        p.add(mkLabel("Controale Sinteza"));
        p.add(mkCCSlider("Modulation",1,0,s->channelModulation[currentChannel]=s));
        p.add(mkCCSlider("Expression",11,127,s->channelExpression[currentChannel]=s));
        p.add(mkCCSlider("Brightness",74,64,s->channelBrightness[currentChannel]=s));
        p.add(mkCCSlider("Resonance",71,64,s->channelResonance[currentChannel]=s));
        p.add(mkCCSlider("Attack",73,64,s->channelAttack[currentChannel]=s));
        p.add(mkCCSlider("Release",72,64,s->channelRelease[currentChannel]=s));
        p.add(mkCCSlider("Vibrato Rate",76,64,s->channelVibratoRate[currentChannel]=s));
        p.add(mkCCSlider("Vibrato Dep",77,0,s->channelVibratoDepth[currentChannel]=s));
        p.add(mkCCSlider("Detune",94,64,s->channelDetune[currentChannel]=s));
        p.add(Box.createVerticalStrut(8));

        p.add(mkLabel("Registru Efecte"));
        activeEffectLabel = new JLabel("Efect activ: Reset");
        activeEffectLabel.setForeground(new Color(200,170,255));
        activeEffectLabel.setFont(new Font("Monospaced",Font.BOLD,10));
        activeEffectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(activeEffectLabel);
        p.add(Box.createVerticalStrut(4));

        JPanel fxGrid = new JPanel(new GridLayout(4,4,2,2));
        fxGrid.setOpaque(false);
        fxGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        fxGrid.setMaximumSize( new Dimension(250,110));
        String[] fxNames = {"Reset","Reverb","Sweep","Trance","Fat","Vibrato","Tremolo","WahWah",
                            "Distort","Delay","Echo","Choir","Portament","Sustain","Bright","Dark"};
        effectButtons = new JButton[fxNames.length];
        for (int i = 0; i < fxNames.length; i++) {
            final String fx = fxNames[i];
            final int idx = i;
            JButton b = new JButton(fx);
            b.setFont(new Font("SansSerif",Font.PLAIN,8));
            b.setMargin(new Insets(1,1,1,1));
            b.setFocusable(false);
            b.setBackground(new Color(55,45,55));
            b.setForeground(new Color(200,170,255));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                applyEffectPreset(fx);
                channelActiveEffect[currentChannel] = idx;
                updateEffectButtons();
            });
            effectButtons[i] = b;
            fxGrid.add(b);
        }
        p.add( fxGrid);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void updateEffectButtons( ) {
        String[] fxNames = {"Reset","Reverb","Sweep","Trance","Fat","Vibrato","Tremolo","WahWah",
                            "Distort","Delay","Echo","Choir","Portament","Sustain","Bright","Dark"};
        int active = channelActiveEffect[currentChannel];
        for(int i = 0; i < effectButtons.length; i++) {
            if(i == active) {
                effectButtons[i].setBackground(new Color(110,80,140));
                effectButtons[i].setForeground(new Color(255,255,255));
            } else {
                effectButtons[i].setBackground(new Color(55,45,55));
                effectButtons[i].setForeground(new Color(200,170,255));
            }
        }
        if (activeEffectLabel != null) {
            activeEffectLabel.setText("Efect activ: " + fxNames[active]);
        }
    }

    interface CCCallback { void set(int val); }

    private JPanel mkCCSlider(String label,int cc, int defVal, CCCallback cb) {
        JPanel p = new JPanel(new BorderLayout(3,0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(250,22));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel( label);
        l.setForeground(TEXT_DIM);
        l.setFont(new Font("SansSerif",Font.PLAIN,9));
        l.setPreferredSize(new Dimension(72,16));
        JSlider s = new JSlider(0,127,defVal);
        s.setOpaque(false);
        s.setFocusable(false);
        JLabel v = new JLabel(""+defVal);
        v.setForeground(TEXT_PRIMARY);
        v.setFont(new Font("Monospaced",Font.PLAIN,9));
        v.setPreferredSize(new Dimension(24,16));
        s.addChangeListener(e -> {
                v.setText("" + s.getValue());
                if(midiChannels[currentChannel] != null)
                    midiChannels[currentChannel].controlChange(cc, s.getValue());
                cb.set(s.getValue());
            });
        p.add(l,BorderLayout.WEST);
        p.add( s,BorderLayout.CENTER);
        p.add(v,BorderLayout.EAST);
        return p;
    }

    private void applyEffectPreset(String fx) {
        int ch = currentChannel;
        MidiChannel mc = midiChannels[ch];
        if(mc==null) return;
        switch(fx) {
            case "Reset": mc.controlChange(91,40);
            mc.controlChange(93,0);
            mc.controlChange(1,0);
            mc.controlChange(74,64);
            mc.controlChange(71,64);
            mc.controlChange(73,64);
            mc.controlChange(72,64);
            mc.controlChange(11,127);
            break;
            case "Reverb": mc.controlChange(91,110);
            mc.controlChange(93,20);
            break;
            case "Sweep": mc.controlChange(74,20);
            mc.controlChange(71,100);
            mc.controlChange( 1,40);
            break;
            case "Trance": mc.controlChange(74,100);
            mc.controlChange(71,90);
            mc.controlChange(91,70);
            mc.controlChange(73,30);
            mc.controlChange(72,50);
            break;
            case "Fat": mc.controlChange(93,80);
            mc.controlChange(91,60);
            mc.controlChange(74,40);
            mc.controlChange( 71,80);
            break;
            case "Vibrato": mc.controlChange(1,80);
            mc.controlChange( 76,90);
            mc.controlChange(77,60);
            break;
            case "Tremolo": mc.controlChange(1,100);
            mc.controlChange(76,110);
            mc.controlChange(77,40);
            break;
            case "WahWah": mc.controlChange(1,90);
            mc.controlChange(74,110);
            mc.controlChange(71,100);
            break;
            case "Distort": mc.controlChange(74,120);
            mc.controlChange(71,120);
            mc.controlChange(91,30);
            break;
            case "Delay": mc.controlChange(91,90);
            mc.controlChange(93,70);
            mc.controlChange(72,100);
            break;
            case "Echo": mc.controlChange(91,120);
            mc.controlChange(93,40);
            mc.controlChange(72,110);
            break;
            case "Choir": mc.controlChange(93,110);
            mc.controlChange(91,80);
            mc.controlChange(1,20);
            break;
            case "Portament": mc.controlChange(65,127);
            mc.controlChange(5,80);
            break;
            case "Sustain": mc.controlChange(64,127);
            mc.controlChange(72,120);
            break;
            case "Bright": mc.controlChange(74,120);
            mc.controlChange(71,30);
            mc.controlChange(11,127);
            break;
            case "Dark": mc.controlChange(74,20);
            mc.controlChange(71,90);
            mc.controlChange(11,90);
            break;
        }
        log("Efect: " + fx + " pe canal " + (ch+1));
    }

    private JPanel createTransformTab() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground( BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));
        p.add(mkLabel("Transformari Creative"));
        p.add(Box.createVerticalStrut(6));
        p.add(mkTransBtn("Transpozitie (+/-)", e -> transposeDialog()));
        p.add(mkTransBtn("Inversiune (Oglindit)", e -> invertSequence()));
        p.add(mkTransBtn("Retrograd (Invers)", e -> retrogradeSequence()));
        p.add(mkTransBtn("Melodie Pi (zecimale)", e -> piMelody()));
        p.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(250,1));
        sep.setForeground( BORDER_COLOR);
        p.add(sep);
        p.add(Box.createVerticalStrut(8));
        JPanel histRow = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        histRow.setOpaque(false);
        histRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        histRow.setMaximumSize(new Dimension(250,30));
        JButton undoBtn=mkBtn("Undo",new Color(80,80,110),11);
        JButton resetBtn=mkBtn("Original",new Color(110,80,50),11);
        undoBtn.addActionListener(e->undoTransform());
        resetBtn.addActionListener(e->resetToOriginal());
        histRow.add(undoBtn);
        histRow.add(resetBtn);
        p.add(histRow);
        p.add(Box.createVerticalStrut(12));
        JButton saveBtn=mkBtn("SALVEAZA MIDI",ACCENT,12);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(250,32));
        saveBtn.addActionListener(e->saveMidiFile());
        p.add( saveBtn);
        p.add(Box.createVerticalStrut(10));
        p.add(mkLabel("Jurnal"));
        transformLogArea = new JTextArea(6,20);
        transformLogArea.setEditable(false);
        transformLogArea.setBackground( new Color(20,20,26));
        transformLogArea.setForeground(new Color(130,180,130));
        transformLogArea.setFont(new Font("Monospaced",Font.PLAIN,10));
        transformLogArea.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
        transformLogArea.setLineWrap(true);
        transformLogArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane( transformLogArea);
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScroll.setMaximumSize(new Dimension(250,300));
        logScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        p.add(logScroll);
        return p;
    }

    class MixerPanel extends JPanel {
        MixerPanel() { setBackground(BG_DARK); setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,1,0,BORDER_COLOR), BorderFactory.createEmptyBorder(4,6,4,6)));
            setLayout( new GridLayout(1,16,3,0));
            for (int i=0; i<NUM_CHANNELS; i++) add(new ChannelStrip(i));
            } }

    class ChannelStrip extends JPanel {
        final int ch;
        JSlider volSlider;
        JPanel vuBar;
        boolean selected=false;
        ChannelStrip(int channel) { this.ch=channel; setLayout(new BorderLayout(0,2));
            setBackground(ch==0?BG_HOVER:BG_ELEMENT);
            setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel topP=new JPanel();
            topP.setLayout(new BoxLayout(topP,BoxLayout.Y_AXIS));
            topP.setOpaque( false);
            JLabel num=new JLabel(""+(ch+1),SwingConstants.CENTER);
            num.setAlignmentX(CENTER_ALIGNMENT);
            num.setFont(new Font("SansSerif",Font.BOLD,11));
            num.setForeground( CH_COLORS[ch]);
            vuBar=new JPanel(){@Override protected void paintComponent(Graphics g){ super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                int h=getHeight(),w=getWidth();
                float lv=channelActivity[ch]/127f;
                int bH=( int)(h*lv);
                if(bH>0){ g2.setPaint(new GradientPaint(0,h,ACCENT_GREEN,0,0,ACCENT_RED)); g2.fillRect(2,h-bH,w-4,bH); }}};
            vuBar.setPreferredSize( new Dimension(0,35));
            vuBar.setMaximumSize( new Dimension(100,35));
            vuBar.setOpaque(true);
            vuBar.setBackground( new Color(20,20,25));
            vuBar.setAlignmentX(CENTER_ALIGNMENT);
            topP.add(Box.createVerticalStrut(2));
            topP.add(num);
            topP.add(vuBar);
            add(topP, BorderLayout.NORTH);

            volSlider=new JSlider(JSlider.VERTICAL,0,127,120);
            volSlider.setOpaque(false);
            volSlider.setFocusable( false);
            volSlider.addChangeListener(e -> {
                    channelVolumes[ch] = volSlider.getValue();
                    if(midiChannels[ch] != null)
                        midiChannels[ch].controlChange(7, volSlider.getValue());
                });
            add(volSlider, BorderLayout.CENTER);

            JPanel botP=new JPanel();
            botP.setLayout(new BoxLayout(botP,BoxLayout.Y_AXIS));
            botP.setOpaque(false);
            JLabel nameL=new JLabel(ch==9?"Drums":"Piano",SwingConstants.CENTER);
            nameL.setAlignmentX(CENTER_ALIGNMENT);
            nameL.setFont(new Font("SansSerif",Font.PLAIN,8));
            nameL.setForeground(TEXT_DIM);
            JPanel ms=new JPanel(new FlowLayout(FlowLayout.CENTER,1,0));
            ms.setOpaque(false);
            JButton mBtn=new JButton("M");
            mBtn.setFont(new Font("SansSerif",Font.BOLD,9));
            mBtn.setMargin(new Insets(0,2,0,2));
            mBtn.setFocusable(false);
            mBtn.setBackground(BG_ELEMENT);
            mBtn.setForeground(TEXT_DIM);
            mBtn.addActionListener(e -> {
                    channelMute[ch] = !channelMute[ch];
                    mBtn.setBackground(channelMute[ch] ? ACCENT_RED : BG_ELEMENT);
                if(midiChannels[ch]!=null) midiChannels[ch].setMute(channelMute[ch]);
                });
            JButton sBtn=new JButton("S");
            sBtn.setFont(new Font("SansSerif",Font.BOLD,9));
            sBtn.setMargin(new Insets(0,2,0,2));
            sBtn.setFocusable(false);
            sBtn.setBackground(BG_ELEMENT);
            sBtn.setForeground(TEXT_DIM);
            sBtn.addActionListener(e -> {
                    channelSolo[ch] = !channelSolo[ch];
                    sBtn.setBackground(channelSolo[ch] ? ACCENT_YELLOW : BG_ELEMENT);
                if(midiChannels[ch]!=null) midiChannels[ch].setSolo(channelSolo[ch]);
                });
            ms.add(mBtn);
            ms.add(sBtn);
            botP.add( nameL);
            botP.add(ms);
            botP.add(Box.createVerticalStrut(2));
            add(botP, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed( MouseEvent e) {
                    selectChannel(ch);
                }
            });
            }
        void setSelected(boolean s) { selected=s; setBackground(s?BG_HOVER:BG_ELEMENT); } }

    static class VisualNote { int note; long startTime; long duration; Color color; int channel;
        VisualNote(int n,long t,int ch,Color c){note=n;startTime=t;channel=ch;color=c;duration=-1;} }

    class NoteVisualizerPanel extends JPanel {
        NoteVisualizerPanel() { setBackground(new Color(18,18,24)); setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER_COLOR)); }

        @Override protected void paintComponent(Graphics g) { super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            long now=System.currentTimeMillis();
            int w=getWidth(),h=getHeight();
            float nr=END_NOTE-START_NOTE;

            int labelWidth = 44;

            g2.setColor( new Color(14, 14, 18));
            g2.fillRect(0, 0, labelWidth, h);
            g2.setColor(new Color(45, 45, 55));
            g2.drawLine(labelWidth, 0, labelWidth, h);

            for (int n = START_NOTE; n <= END_NOTE; n++) {
                int pc = n % 12;
                if (pc == 2 || pc == 4 || pc == 5 || pc == 7 || pc == 9 || pc == 11) {
                    int y = h - (int)((n - START_NOTE) / nr * h);
                    g2.setColor(new Color(26, 26, 32));
                    g2.drawLine(labelWidth, y, w, y);
                }
            }

            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics( );
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            for (int n = START_NOTE; n <= END_NOTE; n++) {
                int pc = n % 12;
                if (pc == 0 || pc == 7) {
                    int y = h - (int)((n - START_NOTE) / nr * h);
                    g2.setColor(pc == 0 ? new Color(55, 55, 75) : new Color(40, 40, 52));
                    g2.drawLine(labelWidth, y, w, y);
                    g2.setColor(pc == 0 ? new Color(170, 180, 210) : new Color(130, 140, 160));
                    String label = SOLFEGE_NAMES[pc] + (n / 12 - 1);
                    int textY = y + ascent / 2 - 1;
                    if(textY < ascent) textY = ascent;
                    if(textY > h - descent) textY = h - descent;
                    g2.drawString(label, 6, textY);
                }
            }

            synchronized(visualNotes) {
                Iterator<VisualNote> it = visualNotes.iterator();
                while(it.hasNext()) {
                    VisualNote vn = it.next();
                    long age = now - vn.startTime;
                    long dur = vn.duration > 0 ? vn.duration : age;
                    if (age > 8000) { it.remove(); continue; }

                    int drawW = w - labelWidth;
                    int x2 = labelWidth + drawW - (int)(age * drawW / 8000.0);
                    int x1 = x2 + Math.max(3, (int)(dur * drawW / 8000.0));
                    if (x1 < labelWidth) { it.remove(); continue; }

                    int y = h - (int)((vn.note - START_NOTE) / nr * (h - 4)) - 2;
                    int bH = Math.max(2, (int)(h / nr * 0.8f));
                    float alpha = Math.max(0.1f, 1f - age / 8000f);

                    g2.setColor(new Color(vn.color.getRed(), vn.color.getGreen(), vn.color.getBlue(),
                        (int)(alpha * 200)));
                    int xs = Math.max(labelWidth, Math.min(x1, x2));
                    int xw = Math.abs(x2 - x1);
                    if (xs + xw > labelWidth) {
                        g2.fillRoundRect(xs, y - bH / 2, xw, bH, 2, 2);
                    }
                }
            }
        }
    }

    private JPanel createPianoContainer() {
        JPanel c=new JPanel(new BorderLayout());
        c.setBackground(BG_DARK);
        c.setBorder(BorderFactory.createMatteBorder(30,0,0,0,BG_DARK));
        pianoPanel=new PianoPanel();
        c.add(pianoPanel,BorderLayout.CENTER);
        c.setPreferredSize(new Dimension(0, 190));
        return c;
        }

    class PianoPanel extends JPanel implements MouseListener, MouseMotionListener {
        int hoveredNote=-1, pressedNote=-1;
        PianoPanel(){ setBackground(new Color(18,18,22)); addMouseListener(this); addMouseMotionListener(this); }
        int whiteCount(){int c=0;for(int n=START_NOTE;n<=END_NOTE;n++) if(!isBlack(n)) c++;return c;}
        int wkw(){return Math.max(10,getWidth()/whiteCount());} int bkw(){return(int)(wkw()*0.58);}
        int wkh(){return getHeight()-18;} int bkh(){return(int)(wkh()*0.62);}

        @Override protected void paintComponent(Graphics g){ super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=wkw(),BW=bkw(),H=wkh(),BH=bkh();
            int x=0;
            for(int note=START_NOTE;note<=END_NOTE;note++){ if(!isBlack(note)){ boolean a=activeNotes.contains(note),h2=note==hoveredNote;
                boolean inM1,inM2;
                if (useMidiInput) {
                    inM1 = note >= midiInputLowestNote && note <= midiInputHighestNote;
                    inM2 = false;
                } else {
                    inM1 = note >= (currentOctaveManual1+1)*12 && note <= (currentOctaveManual1+1)*12+12;
                    inM2 = note >= (currentOctaveManual2+1)*12 && note <= (currentOctaveManual2+1)*12+12;
                }
                if(a) g2.setPaint(new GradientPaint(x,0,CH_COLORS[currentChannel],x,H,CH_COLORS[currentChannel].darker()));
                else if(h2) g2.setColor(new Color(230,230,240));
                else if(inM1 && inM2) g2.setPaint(new GradientPaint(x,0,new Color(210,240,210),x,H,new Color(185,215,185)));
                else if(inM1) g2.setPaint(new GradientPaint(x,0,new Color(210,225,245),x,H,new Color(185,200,225)));
                else if(inM2) g2.setPaint(new GradientPaint(x,0,new Color(245,225,210),x,H,new Color(225,200,185)));
                else g2.setPaint(new GradientPaint(x,0,new Color(250,250,252),x,H,new Color(220,220,225)));
                g2.fillRoundRect(x+1,0,W-2,H,2,4);
                g2.setColor(new Color(170,170,180));
                g2.drawRoundRect(x+1,0,W-2,H,2,4);
                if(note%12==0){g2.setColor(new Color(140,140,150));g2.setFont(new Font("SansSerif",Font.PLAIN,9));g2.drawString("Do"+(note/12-1),x+3,H-3);}
                x+=W;
                }}
            x=0;
            for(int note=START_NOTE;note<=END_NOTE;note++){ if(!isBlack(note)){
                if(note+1<=END_NOTE&&isBlack(note+1)){ int bx=x+W-BW/2; boolean a=activeNotes.contains(note+1),h2=(note+1)==hoveredNote;
                    boolean bInM1, bInM2;
                    if(useMidiInput) {
                        bInM1 = (note+1) >= midiInputLowestNote && (note+1) <= midiInputHighestNote;
                        bInM2 = false;
                    } else {
                        bInM1 = (note+1) >= (currentOctaveManual1+1)*12 && (note+1) <= (currentOctaveManual1+1)*12+12;
                        bInM2 = (note+1) >= (currentOctaveManual2+1)*12 && (note+1) <= (currentOctaveManual2+1)*12+12;
                    }
                    if(a) g2.setPaint(new GradientPaint(bx,0,CH_COLORS[currentChannel].darker(),bx,BH,CH_COLORS[currentChannel].darker().darker()));
                    else if(h2) g2.setColor(new Color(55,55,65));
                    else if( bInM1 && bInM2) g2.setPaint(new GradientPaint(bx,0,new Color(55,70,55),bx,BH,new Color(30,42,30)));
                    else if(bInM1) g2.setPaint(new GradientPaint(bx,0,new Color(50,55,70),bx,BH,new Color(28,32,45)));
                    else if( bInM2) g2.setPaint(new GradientPaint(bx,0,new Color(70,55,50),bx,BH,new Color(45,32,28)));
                    else g2.setPaint(new GradientPaint(bx,0,new Color(40,40,45),bx,BH,new Color(20,20,22)));
                    g2.fillRoundRect(bx,0,BW,BH,2,3);
                    g2.setColor( new Color(15,15,18));
                    g2.drawRoundRect(bx,0,BW,BH,2,3);
                    }
                x+=W;
                }}
            drawZone(g2,currentOctaveManual1,new Color(100,180,255,50),"M1",W,H);
            drawZone(g2,currentOctaveManual2,new Color(255,180,100,50),"M2",W,H);
            }
        void drawZone(Graphics2D g2,int oct,Color c,String lbl,int W,int H){ int sn=(oct+1)*12;
            int x1=noteX(sn,W),x2=noteX(sn+13,W);
            if(x1>=0&&x2>=0){ g2.setColor(c); g2.fillRect(x1,H+1,x2-x1,16);
                g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),150));
                g2.setFont(new Font("SansSerif",Font.BOLD,9));
                g2.drawString( lbl,x1+3,H+12);
                }}
        int noteX(int t,int W){int x=0;for(int n=START_NOTE;n<=END_NOTE;n++){if(!isBlack(n)){if(n==t) return x;x+=W;}}return-1;}
        int noteAt( int mx,int my){int W=wkw(),BW=bkw(),BH=bkh();
            if(my<BH){int x=0;for(int n=START_NOTE;n<=END_NOTE;n++){if(!isBlack(n)){if(n+1<=END_NOTE&&isBlack(n+1)){int bx=x+W-BW/2;if(mx>=bx&&mx<bx+BW) return n+1;}x+=W;}}}
            int x=0;
            for(int n=START_NOTE;n<=END_NOTE;n++){if(!isBlack(n)){if(mx>=x&&mx<x+W) return n;x+=W;}}return-1;
            }

        @Override public void mousePressed(MouseEvent e){int n=noteAt(e.getX(),e.getY());if(n>=0){pressedNote=n;noteOn(n,currentVelocity);}}

        @Override public void mouseReleased(MouseEvent e){if(pressedNote>=0){noteOff(pressedNote);pressedNote=-1;}}

        @Override public void mouseDragged(MouseEvent e){int n=noteAt(e.getX(),e.getY());if(n!=pressedNote&&n>=0){if(pressedNote>=0)noteOff(pressedNote);pressedNote=n;noteOn(n,currentVelocity);}}

        @Override public void mouseMoved(MouseEvent e){hoveredNote=noteAt(e.getX(),e.getY());repaint();}

        @Override public void mouseClicked(MouseEvent e){} @Override public void mouseEntered(MouseEvent e){} @Override public void mouseExited(MouseEvent e){hoveredNote=-1;repaint();} }

    private JButton mkBtn( String t,Color bg,int fs){JButton b=new JButton(t);b.setBackground(bg);b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setFont(new Font("SansSerif",Font.BOLD,fs));
        b.setBorder(BorderFactory.createEmptyBorder(5,12,5,12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
        }

    private JButton mkTransBtn(String t,ActionListener a){JButton b=new JButton("  "+t);b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize( new Dimension(250,28));
        b.setBackground(BG_ELEMENT);
        b.setForeground(TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setFont(new Font("SansSerif",Font.PLAIN,11));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),BorderFactory.createEmptyBorder(4,8,4,8)));
        b.addActionListener(a);
        return b;
        }

    private JLabel mkLabel(String t){JLabel l=new JLabel(t);l.setForeground(new Color(180,170,130));
        l.setFont(new Font("SansSerif",Font.BOLD,11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder( BorderFactory.createEmptyBorder(0,0,3,0));
        return l;
        }

    interface SSetup{void s( JSlider s);}

    private JPanel mkSlider(String label,int min,int max,int val,SSetup setup){
        JPanel p=new JPanel(new BorderLayout(4,0));
        p.setOpaque(false);
        p.setMaximumSize( new Dimension(250,28));
        p.setAlignmentX( Component.LEFT_ALIGNMENT);
        JLabel l=new JLabel(label);
        l.setForeground(TEXT_DIM);
        l.setFont(new Font("SansSerif",Font.PLAIN,10));
        l.setPreferredSize(new Dimension(58,18));
        JSlider s=new JSlider(min,max,val);
        s.setOpaque(false);
        s.setFocusable(false);
        JLabel v=new JLabel(""+val);
        v.setForeground(TEXT_PRIMARY);
        v.setFont(new Font("Monospaced",Font.PLAIN,10));
        v.setPreferredSize(new Dimension(28,18));
        s.addChangeListener(e->v.setText(""+s.getValue()));
        setup.s(s);
        p.add(l,BorderLayout.WEST);
        p.add(s,BorderLayout.CENTER);
        p.add(v,BorderLayout.EAST);
        return p;
        }

    private void initKeyboardListener(){KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e->{
        if(learnWindow != null && learnWindow.isDisplayable() && learnWindow.isActive()) return false;
        if(e.getID()==KeyEvent.KEY_PRESSED) handleKeyPress(e);
        else if(e.getID()==KeyEvent.KEY_RELEASED) handleKeyRelease(e);
        return false;
        });}

    private void handleKeyPress( KeyEvent e){int kc=e.getKeyCode();if(pressedKeys.contains(kc)) return;pressedKeys.add(kc);
        if(e.isControlDown()){if(kc==KeyEvent.VK_Z){undoTransform();return;}if(kc==KeyEvent.VK_S){saveMidiFile();return;}}
        if(useMidiInput) {
            if(kc==KeyEvent.VK_LEFT){shiftMidiInputRange(-12); return;}
            if(kc==KeyEvent.VK_RIGHT){shiftMidiInputRange(12); return;}
            if(kc>=KeyEvent.VK_F1&&kc<=KeyEvent.VK_F12){selectChannel(kc-KeyEvent.VK_F1);return;}
            return;
        }
        if(kc==KeyEvent.VK_LEFT){releaseAllNotes();currentOctaveManual1=Math.max(2,currentOctaveManual1-1);pianoPanel.repaint();updateOctaveDisplay();return;}
        if(kc==KeyEvent.VK_RIGHT){releaseAllNotes();currentOctaveManual1=Math.min(7,currentOctaveManual1+1);pianoPanel.repaint();updateOctaveDisplay();return;}
        if(kc==KeyEvent.VK_DOWN){releaseAllNotes();currentOctaveManual2=Math.max(2,currentOctaveManual2-1);pianoPanel.repaint();updateOctaveDisplay();return;}
        if(kc==KeyEvent.VK_UP){releaseAllNotes();currentOctaveManual2=Math.min(7,currentOctaveManual2+1);pianoPanel.repaint();updateOctaveDisplay();return;}
        if(kc==KeyEvent.VK_PERIOD){currentManualForChannelSelect=1; selectChannel(manual1Channel); return;}
        if( kc==KeyEvent.VK_SLASH){currentManualForChannelSelect=2; selectChannel(manual2Channel); return;}
        if(kc>=KeyEvent.VK_F1&&kc<=KeyEvent.VK_F12){selectChannel(kc-KeyEvent.VK_F1);return;}
        if( manual1Map.containsKey(kc)){int note=(currentOctaveManual1+1)*12+manual1Map.get(kc);if(note>=0&&note<=127) noteOnCh(note,currentVelocity,manual1Channel);return;}
        if(manual2Map.containsKey(kc)){int note=(currentOctaveManual2+1)*12+manual2Map.get(kc);if(note>=0&&note<=127) noteOnCh(note,currentVelocity,manual2Channel);return;}
        }

    private void handleKeyRelease(KeyEvent e){int kc=e.getKeyCode();pressedKeys.remove(kc);
        if(useMidiInput) return;
        if(manual1Map.containsKey(kc)){int note=(currentOctaveManual1+1)*12+manual1Map.get(kc);if(note>=0&&note<=127) noteOffCh(note,manual1Channel);}
        if(manual2Map.containsKey(kc)){int note=(currentOctaveManual2+1)*12+manual2Map.get(kc);if(note>=0&&note<=127) noteOffCh(note,manual2Channel);}}

    private void noteOn(int note,int velocity){if(midiChannels[currentChannel]!=null&&note>=0&&note<=127){
        midiChannels[currentChannel].noteOn(note,velocity);
        activeNotes.add(note);
        channelActivity[currentChannel]=Math.min(127,velocity+20);
        pianoPanel.repaint();
        updateNoteDisplay();
        visualNotes.add(new VisualNote(note,System.currentTimeMillis(),currentChannel,CH_COLORS[currentChannel]));
        if(isRecording&&recordingTrack!=null){try{long tick=millisToTick(System.currentTimeMillis()-recordingStartTime);
            ShortMessage msg=new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_ON,currentChannel,note,velocity);
            recordingTrack.add(new MidiEvent(msg,tick));
            }catch( InvalidMidiDataException ex){}}}}

    private void noteOff(int note){if(midiChannels[currentChannel]!=null&&note>=0&&note<=127){
        midiChannels[currentChannel].noteOff(note);
        activeNotes.remove(note);
        pianoPanel.repaint();
        updateNoteDisplay();
        synchronized(visualNotes){for(int i=visualNotes.size()-1;i>=0;i--){VisualNote vn=visualNotes.get(i);if(vn.note==note&&vn.duration<0){vn.duration=System.currentTimeMillis()-vn.startTime;break;}}}
        if(isRecording&&recordingTrack!=null){try{long tick=millisToTick(System.currentTimeMillis()-recordingStartTime);
            ShortMessage msg=new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_OFF,currentChannel,note,0);
            recordingTrack.add(new MidiEvent(msg,tick));
            }catch(InvalidMidiDataException ex){}}}}

    private void releaseAllNotes(){for(int note:new HashSet<>(activeNotes)){
            for(int c=0;c<NUM_CHANNELS;c++) if(midiChannels[c]!=null) midiChannels[c].noteOff(note);
            }
        activeNotes.clear();
        pressedKeys.clear( );
        pianoPanel.repaint();
        updateNoteDisplay();
        }

    private void noteOnCh(int note,int velocity,int ch){if(midiChannels[ch]!=null&&note>=0&&note<=127){
        midiChannels[ch].noteOn(note,velocity);
        activeNotes.add(note);
        channelActivity[ch]=Math.min(127,velocity+20);
        pianoPanel.repaint();
        updateNoteDisplay();
        visualNotes.add(new VisualNote(note,System.currentTimeMillis(),ch,CH_COLORS[ch]));
        if(isRecording&&recordingTrack!=null){try{long tick=millisToTick(System.currentTimeMillis()-recordingStartTime);
            ShortMessage msg=new ShortMessage();
            msg.setMessage( ShortMessage.NOTE_ON,ch,note,velocity);
            recordingTrack.add(new MidiEvent(msg,tick));
            }catch(InvalidMidiDataException ex){}}}}

    private void noteOffCh(int note,int ch){if(midiChannels[ch]!=null&&note>=0&&note<=127){
        midiChannels[ch].noteOff(note);
        activeNotes.remove(note);
        pianoPanel.repaint( );
        updateNoteDisplay();
        synchronized(visualNotes){for(int i=visualNotes.size()-1;i>=0;i--){VisualNote vn=visualNotes.get(i);if(vn.note==note&&vn.channel==ch&&vn.duration<0){vn.duration=System.currentTimeMillis()-vn.startTime;break;}}}
        if(isRecording&&recordingTrack!=null){try{long tick=millisToTick(System.currentTimeMillis()-recordingStartTime);
            ShortMessage msg=new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_OFF,ch,note,0);
            recordingTrack.add( new MidiEvent(msg,tick));
            }catch(InvalidMidiDataException ex){}}}}

    private void updateNoteDisplay(){if(activeNotes.isEmpty()){noteDisplayLabel.setText("---");chordDisplayLabel.setText("Note active: ---");}
        else{List<Integer> sorted=new ArrayList<>(activeNotes);Collections.sort(sorted);StringBuilder sb=new StringBuilder();
            for(int n:sorted){if(sb.length()>0)sb.append(" ");sb.append(SOLFEGE_NAMES[n%12]).append(n/12-1);}
            noteDisplayLabel.setText(sb.toString());
            chordDisplayLabel.setText("Note: "+sb);
            }}

    private void updateOctaveDisplay(){
        if (m1Label == null || m2Label == null) return;
        if(useMidiInput) {
            int oct = midiInputLowestNote / 12 - 1;
            m1Label.setText("M1: octava " + oct);
            m1Label.setForeground(ACCENT);
            m2Label.setText("M2: inactiv");
            m2Label.setForeground(TEXT_DIM);
        } else {
            m1Label.setText("M1: octava " + currentOctaveManual1);
            m1Label.setForeground(ACCENT);
            m2Label.setText("M2: octava " + currentOctaveManual2);
            m2Label.setForeground(ACCENT);
        }
    }

    private void updateManualChannelLabels() {
        if (m1ChLabel == null || m2ChLabel == null) return;
        if(useMidiInput) {
            m1ChLabel.setText("M1: Canal " + (manual1Channel + 1) + (manual1Channel == 9 ? " (Perc)" : ""));
            m1ChLabel.setForeground(new Color(100, 180, 255));
            m2ChLabel.setText("M2: inactiv");
            m2ChLabel.setForeground(TEXT_DIM);
        } else {
            String m1Marker = currentManualForChannelSelect == 1 ? " ●" : "";
            String m2Marker = currentManualForChannelSelect == 2 ? " ●" : "";
            m1ChLabel.setText("M1: Canal " + (manual1Channel + 1) + (manual1Channel == 9 ? " (Perc)" : "") + m1Marker);
            m1ChLabel.setForeground(new Color(100, 180, 255));
            m2ChLabel.setText("M2: Canal " + (manual2Channel + 1) + (manual2Channel == 9 ? " (Perc)" : "") + m2Marker);
            m2ChLabel.setForeground(new Color(255, 180, 100));
        }
    }

    private void selectChannel(int ch){if(ch<0||ch>=NUM_CHANNELS) return; currentChannel=ch;
        if(currentManualForChannelSelect == 2 && !useMidiInput) {
            manual2Channel = ch;
        } else {
            manual1Channel = ch;
        }
        channelLabel.setText("Canal "+(ch+1)+(ch==9?" (Percutie)":" - "+GM_INSTRUMENTS[channelInstruments[ch]]));
        int prog_ch = channelInstruments[ch]; categoryCombo.setSelectedIndex( prog_ch / 8); instrumentCombo.setSelectedIndex(prog_ch % 8);
        if(volumeSlider!=null) volumeSlider.setValue(channelVolumes[ch]);
        if(panSlider!=null) panSlider.setValue(channelPans[ch]);
        if(reverbSlider!=null) reverbSlider.setValue(channelReverb[ch]);
        if(chorusSlider!=null) chorusSlider.setValue(channelChorus[ch]);
        if(effectButtons!=null) updateEffectButtons();
        updateManualChannelLabels();

        for(Component c:mixerPanel.getComponents()) if(c instanceof ChannelStrip) ((ChannelStrip)c).setSelected(((ChannelStrip)c).ch==ch);
        }

    private void startVUTimer( ){vuTimer=new Timer();vuTimer.scheduleAtFixedRate(new TimerTask(){@Override public void run(){
        for(int i=0;i<NUM_CHANNELS;i++) channelActivity[i]=Math.max(0,channelActivity[i]-6);
        SwingUtilities.invokeLater(()->{if(mixerPanel!=null) mixerPanel.repaint();if(vizPanel!=null) vizPanel.repaint();});
        }},0,40);}

    private void toggleRecording( ){if(isRecording) stopRecording(); else startRecording();}

    private void startRecording(){try{
        JPanel sp=new JPanel(new GridLayout(2,2,5,5));
        sp.setOpaque(false);

        JTextField bpmF=new JTextField(""+recordingBPM);
        JComboBox<String> modeC=new JComboBox<>(new String[]{"Inregistrare noua","Suprapunere"});
        sp.add(new JLabel("Tempo (BPM):"));
        sp.add(bpmF);
        sp.add(new JLabel("Mod:"));
        sp.add(modeC);
        if(JOptionPane.showConfirmDialog(this,sp,"Setari Inregistrare",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
        recordingBPM=Math.max(20,Math.min(300,Integer.parseInt(bpmF.getText().trim())));
        boolean overlay=modeC.getSelectedIndex()==1&&currentSequence!=null;
        if(overlay){recordedSequence=currentSequence;recordingResolution=currentSequence.getResolution();}
        else{recordedSequence=new Sequence(Sequence.PPQ,recordingResolution);}
        recordingTrack=recordedSequence.createTrack();
        ShortMessage pc=new ShortMessage();
        pc.setMessage(ShortMessage.PROGRAM_CHANGE,currentChannel,channelInstruments[currentChannel],0);
        recordingTrack.add(new MidiEvent(pc,0));
        int mpqn=60_000_000/recordingBPM;
        MetaMessage tempoMsg=new MetaMessage(0x51,new byte[]{(byte)((mpqn>>16)&0xFF),(byte)((mpqn>>8)&0xFF),(byte)(mpqn&0xFF)},3);
        recordingTrack.add( new MidiEvent(tempoMsg,0));
        isRecording=true;
        recordingStartTime=System.currentTimeMillis();
        beatCount=0;
        recordBtn.setText("STOP");
        recordBtn.setBackground(new Color(255,40,40));
        statusLabel.setText("REC");
        statusLabel.setForeground(ACCENT_RED);
        if( metronomeEnabled) startMetronome();
        log("REC: "+recordingBPM+" BPM, canal "+(currentChannel+1));
    }catch(Exception ex){log("Eroare: "+ex.getMessage());
    }}

    private void stopRecording(){if(!isRecording) return; isRecording=false; stopMetronome();
        long dur=System.currentTimeMillis()-recordingStartTime;
        try{recordingTrack.add(new MidiEvent(new MetaMessage(0x2F,new byte[0],0),millisToTick(dur)+recordingResolution));}catch(Exception ex){}
        int nc=0;
        for(int i=0;i<recordingTrack.size();i++){MidiMessage msg=recordingTrack.get(i).getMessage();
            if(msg instanceof ShortMessage&&((ShortMessage)msg).getCommand()==ShortMessage.NOTE_ON) nc++;
            }
        currentSequence=recordedSequence;
        try{sequencer.setSequence(currentSequence);}catch(InvalidMidiDataException ex){}
        saveOriginal();
        recordBtn.setText("REC");
        recordBtn.setBackground( ACCENT_RED);
        statusLabel.setText("Inregistrat");
        statusLabel.setForeground(ACCENT_GREEN);
        log("STOP: "+nc+" note, "+formatTime(dur/1000));
        updatePlaybackButtons();
        }

    private void startMetronome(){stopMetronome();long interval=60_000L/recordingBPM;
        metronomeTimer=new Timer();
        metronomeTimer.scheduleAtFixedRate(new TimerTask(){@Override public void run(){
            if( midiChannels[9]!=null) midiChannels[9].noteOn(beatCount%4==0?76:77,80);
            int beat=beatCount%4;
            SwingUtilities.invokeLater(()->{for(int i=0;i<beatIndicatorPanel.getComponentCount();i++)
                beatIndicatorPanel.getComponent(i).setBackground(i==beat?(beat==0?ACCENT_RED:ACCENT_YELLOW):new Color(50,50,60));
                beatIndicatorPanel.repaint();
                if(beatCountLabel!=null) beatCountLabel.setText("Beat: "+beatCount+" | "+(beat+1)+"/4");
                });beatCount++;}},0,interval);}

    private void stopMetronome(){if(metronomeTimer!=null){metronomeTimer.cancel();metronomeTimer=null;}
        SwingUtilities.invokeLater(()->{for(int i=0;i<beatIndicatorPanel.getComponentCount();i++) beatIndicatorPanel.getComponent(i).setBackground(new Color(50,50,60));});
        }

    private long millisToTick(long ms){return ms*recordingResolution*recordingBPM/60_000;}

    private void loadMidiFile(){JFileChooser ch=new JFileChooser();ch.setFileFilter(new FileNameExtensionFilter("MIDI (*.mid,*.midi)","mid","midi"));
        if(ch.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){try{File f=ch.getSelectedFile();currentSequence=MidiSystem.getSequence(f);
            analyzeSequence(currentSequence);
            stripProgramChanges(currentSequence);
            sequencer.setSequence(currentSequence);
            log("Incarcat: "+f.getName()+" | "+formatTime(currentSequence.getMicrosecondLength()/1_000_000)+" | "+currentSequence.getTracks().length+" piste");
            statusLabel.setText(f.getName());

            saveOriginal();
            updatePlaybackButtons();
        }catch(Exception ex){log("Eroare: "+ex.getMessage());
        }}}

    
    private void stripProgramChanges(Sequence seq){
        for(Track t : seq.getTracks()){
            for(int i = t.size() - 1; i >= 0; i--){
                MidiMessage msg = t.get(i).getMessage();
                if(msg instanceof ShortMessage){
                    ShortMessage sm = (ShortMessage) msg;
                    int cmd = sm.getCommand();
                    if(cmd == ShortMessage.PROGRAM_CHANGE){
                        t.remove(t.get(i));
                    } else if(cmd == ShortMessage.CONTROL_CHANGE){
                        int cc = sm.getData1();
                        // 7=volum, 10=pan, 91=reverb, 93=chorus
                        if(cc == 7 || cc == 10 || cc == 91 || cc == 93){
                            t.remove(t.get(i));
                        }
                    }
                }
            }
        }
    }

    private void applyInstrumentsToSynth(){
        for(int c = 0; c < NUM_CHANNELS; c++){
            if(midiChannels[c] != null){
                midiChannels[c].programChange(channelInstruments[c]);
                midiChannels[c].controlChange(7, channelVolumes[c]);
                midiChannels[c].controlChange(10, channelPans[c]);
                midiChannels[c].controlChange(91, channelReverb[c]);
                midiChannels[c].controlChange(93, channelChorus[c]);
            }
        }
    }

    private void analyzeSequence(Sequence seq){Map<Integer,Integer> cn=new HashMap<>();
        for(Track t:seq.getTracks()) for(int i=0;i<t.size();i++){MidiMessage msg=t.get(i).getMessage();
            if(msg instanceof ShortMessage){ShortMessage sm=(ShortMessage)msg;
                if(sm.getCommand()==ShortMessage.NOTE_ON) cn.merge(sm.getChannel(),1,Integer::sum);
                if(sm.getCommand()==ShortMessage.PROGRAM_CHANGE) channelInstruments[sm.getChannel()]=sm.getData1();
                if(sm.getCommand()==ShortMessage.CONTROL_CHANGE){
                    int ch=sm.getChannel(), cc=sm.getData1(), val=sm.getData2();
                    if(cc==7) channelVolumes[ch]=val;
                    else if(cc==10) channelPans[ch]=val;
                    else if(cc==91) channelReverb[ch]=val;
                    else if(cc==93) channelChorus[ch]=val;
                }
                }}
        StringBuilder sb=new StringBuilder("Canale: ");
        for(Map.Entry<Integer,Integer> e:cn.entrySet()) sb.append("Ch").append(e.getKey()+1).append("(").append(e.getValue()).append(") ");
        log(sb.toString());
        }

    private void playMidi(){if(currentSequence==null){log("Incarcati/inregistrati mai intai.");return;}
        try{
            applyInstrumentsToSynth();
            if(!isPaused) {
                sequencer.setMicrosecondPosition(0);
            } else {
                sequencer.setMicrosecondPosition(pausePosition);
            }
            sequencer.setTempoInBPM(tempoSlider.getValue());
            sequencer.start();
            isPlaying=true;
            isPaused=false;
            updatePlaybackButtons();
            startProgressTimer();
            statusLabel.setText("PLAY");
            statusLabel.setForeground(ACCENT_GREEN);
        }catch(Exception ex){log("Eroare: "+ex.getMessage());
        }}

    private void pauseMidi(){if(sequencer.isRunning()){pausePosition=sequencer.getMicrosecondPosition();sequencer.stop();
        isPlaying=false;
        isPaused=true;
        updatePlaybackButtons();
        statusLabel.setText("PAUSE");
        statusLabel.setForeground(ACCENT_YELLOW);
        }}

    private void stopMidi(){sequencer.stop();sequencer.setMicrosecondPosition(0);isPlaying=false;isPaused=false;pausePosition=0;
        updatePlaybackButtons();
        stopProgressTimer();
        progressBar.setValue(0);
        progressBar.setString("--:-- / --:--");
        statusLabel.setText("STOP");
        statusLabel.setForeground(TEXT_DIM);
        }

    private void updatePlaybackButtons(){playBtn.setEnabled(!isPlaying);pauseBtn.setEnabled(isPlaying);stopBtn.setEnabled(isPlaying||isPaused);}

    private void startProgressTimer( ){stopProgressTimer();progressTimer=new Timer();progressTimer.scheduleAtFixedRate(new TimerTask(){@Override public void run(){
        SwingUtilities.invokeLater(()->{if(sequencer.isRunning()&&currentSequence!=null){long pos=sequencer.getMicrosecondPosition()/1_000_000;long total=currentSequence.getMicrosecondLength()/1_000_000;
            progressBar.setValue(total>0?(int)(pos*100/total):0);
            progressBar.setString(formatTime(pos)+" / "+formatTime(total));
            }});}},0,250);}

    private void stopProgressTimer(){if(progressTimer!=null){progressTimer.cancel();progressTimer=null;}}

    private Sequence cloneSequence(Sequence src) throws InvalidMidiDataException{
        Sequence c=new Sequence(src.getDivisionType(),src.getResolution());
        for(Track t:src.getTracks()){Track nt=c.createTrack();for(int i=0;i<t.size();i++){MidiEvent ev=t.get(i);nt.add(new MidiEvent(ev.getMessage(),ev.getTick()));}}return c;
        }

    private void pushHistory(){if(currentSequence==null) return;try{historyStack.push(cloneSequence(currentSequence));
        if(historyStack.size()>MAX_HISTORY)((ArrayDeque<Sequence>)historyStack).removeLast();
        }catch(InvalidMidiDataException ex){}}

    private void saveOriginal(){if(currentSequence==null) return;try{originalSequence=cloneSequence(currentSequence);historyStack.clear();}catch(InvalidMidiDataException ex){}}

    private void undoTransform(){if(historyStack.isEmpty()){log("Nimic de anulat.");return;}
        try{currentSequence=historyStack.pop();sequencer.setSequence(currentSequence);log("Undo ("+historyStack.size()+" in istoric)");}catch(InvalidMidiDataException ex){log("Eroare: "+ex.getMessage());}}

    private void resetToOriginal(){if(originalSequence==null){log("Nu exista original.");return;}
        try{pushHistory();currentSequence=cloneSequence(originalSequence);sequencer.setSequence(currentSequence);log("Resetat la original.");}catch(InvalidMidiDataException ex){log("Eroare: "+ex.getMessage());}}

    private void transposeDialog( ) {
        if(currentSequence == null) return;

        String input = JOptionPane.showInputDialog(this,
            "Semitonuri (+/-):", "Transpozitie", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;

        try {
            int s = Integer.parseInt(input.trim());
            pushHistory();

            Sequence n = new Sequence(currentSequence.getDivisionType(),currentSequence.getResolution());

            for (Track t : currentSequence.getTracks()) {
                Track nt = n.createTrack();

                for (int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get(i);
                    MidiMessage msg = ev.getMessage();

                    if(msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;

                        if (sm.getCommand() == ShortMessage.NOTE_ON ||
                            sm.getCommand() == ShortMessage.NOTE_OFF) {

                            int newNote = Math.max(0, Math.min(127, sm.getData1() + s));
                            ShortMessage nm = new ShortMessage();
                            nm.setMessage( sm.getCommand(), sm.getChannel(), newNote, sm.getData2());
                            nt.add(new MidiEvent(nm,ev.getTick()));
                            continue;
                        }
                    }
                    nt.add(new MidiEvent(msg, ev.getTick()));
                }
            }

            currentSequence = n;
            sequencer.setSequence( n);
            log("Transpozitie: " + (s > 0 ? "+" : "") + s);

        } catch (Exception ex) {
            log("Eroare: " + ex.getMessage());
        }
    }

    private void invertSequence() {
        if (currentSequence == null) return;

        int pivot;
        int autoPivot = calcPivot(currentSequence);
        String autoName = SOLFEGE_NAMES[autoPivot % 12] + (autoPivot / 12 - 1);

        JPanel dlg = new JPanel(new GridLayout(0, 1, 4, 4));
        dlg.setPreferredSize(new Dimension(340, 130));

        JRadioButton autoOpt = new JRadioButton("Automat: " + autoName, true);
        JRadioButton customOpt = new JRadioButton("Alege pivotul:");
        ButtonGroup grp = new ButtonGroup();
        grp.add(autoOpt);
        grp.add(customOpt);
        dlg.add(autoOpt);
        dlg.add(customOpt);

        JPanel noteSel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JComboBox<String> noteCombo = new JComboBox<>(SOLFEGE_NAMES);
        noteCombo.setSelectedIndex( autoPivot % 12);
        JComboBox<String> octCombo = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6", "7"});
        octCombo.setSelectedItem(String.valueOf(autoPivot / 12 - 1));
        noteSel.add(new JLabel("Nota:"));
        noteSel.add(noteCombo);
        noteSel.add(new JLabel("Octava:"));
        noteSel.add(octCombo);
        dlg.add(noteSel);

        noteCombo.setEnabled( false);
        octCombo.setEnabled(false);
        autoOpt.addActionListener(e -> {
            noteCombo.setEnabled(false);
            octCombo.setEnabled(false);
        });
        customOpt.addActionListener(e -> {
            noteCombo.setEnabled(true);
            octCombo.setEnabled(true);
        });

        int result = JOptionPane.showConfirmDialog(this, dlg, "Inversiune",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(result != JOptionPane.OK_OPTION) return;

        if (autoOpt.isSelected()) {
            pivot = autoPivot;
        } else {
            int noteClass = noteCombo.getSelectedIndex();
            int octave = Integer.parseInt((String) octCombo.getSelectedItem());
            pivot = (octave + 1) * 12 + noteClass;
        }

        try {
            pushHistory();

            Sequence n = new Sequence( currentSequence.getDivisionType(), currentSequence.getResolution());

            for (Track t : currentSequence.getTracks()) {
                Track nt = n.createTrack();

                for (int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get(i);
                    MidiMessage msg = ev.getMessage( );

                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;

                        if (sm.getCommand() == ShortMessage.NOTE_ON ||
                            sm.getCommand() == ShortMessage.NOTE_OFF) {

                            int inverted = Math.max(0, Math.min(127, 2 * pivot - sm.getData1()));
                            ShortMessage nm = new ShortMessage();
                            nm.setMessage(sm.getCommand(), sm.getChannel(), inverted, sm.getData2());
                            nt.add(new MidiEvent(nm,ev.getTick()));
                            continue;
                        }
                    }
                    nt.add(new MidiEvent(msg, ev.getTick()));
                }
            }

            currentSequence = n;
            sequencer.setSequence(n);
            log("Inversiune (pivot " + SOLFEGE_NAMES[pivot % 12] + (pivot / 12 - 1) + ", MIDI " + pivot + ")");

        } catch(Exception ex) {
            log("Eroare: " + ex.getMessage());
        }
    }

    private void retrogradeSequence() {
        if (currentSequence == null) return;

        long max = 0;
        for (Track t : currentSequence.getTracks()) {
            if (t.size() > 0) {
                max = Math.max(max,t.get(t.size() - 1).getTick());
            }
        }

        int res = currentSequence.getResolution();
        double totalBeats = max / (double) res;

        JPanel dlg = new JPanel(new GridLayout(0, 1, 4, 4));
        dlg.setPreferredSize(new Dimension(360, 120));

        JRadioButton autoOpt = new JRadioButton(
            String.format("Automat: %.2f batai", totalBeats), true);
        JRadioButton customOpt = new JRadioButton("Alege punctul:");
        ButtonGroup grp = new ButtonGroup( );
        grp.add(autoOpt);
        grp.add(customOpt);
        dlg.add(autoOpt);
        dlg.add(customOpt);

        JPanel pointSel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JTextField beatsField = new JTextField(String.format("%.2f", totalBeats / 2), 8);
        pointSel.add(new JLabel("Batai:"));
        pointSel.add( beatsField);
        dlg.add(pointSel);

        beatsField.setEnabled(false);
        autoOpt.addActionListener(e -> beatsField.setEnabled(false));
        customOpt.addActionListener(e -> beatsField.setEnabled(true));

        int result = JOptionPane.showConfirmDialog(this, dlg, "Retrograd",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        long mirrorTick;
        if(autoOpt.isSelected()) {
            mirrorTick = max;
        } else {
            try {
                double beats = Double.parseDouble(beatsField.getText().trim().replace(',', '.'));
                mirrorTick = (long)(beats * 2 * res);
            } catch (NumberFormatException ex) {
                log("Valoare invalida.");
                return;
            }
        }

        try {
            pushHistory();

            Sequence n = new Sequence(currentSequence.getDivisionType(), currentSequence.getResolution());

            for(Track t : currentSequence.getTracks()) {
                Track nt = n.createTrack();

                Map<Integer, List<Integer>> openNotes = new HashMap<>();
                int[] pairedOff = new int[t.size()];
                int[] pairedOn = new int[t.size()];
                Arrays.fill(pairedOff, -1);
                Arrays.fill( pairedOn, -1);

                for(int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get(i);
                    MidiMessage msg = ev.getMessage();
                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        int key = sm.getData1() * 16 + sm.getChannel();

                        if(sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                            openNotes.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
                        } else if (sm.getCommand() == ShortMessage.NOTE_OFF ||
                                   (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                            List<Integer> opens = openNotes.get(key);
                            if(opens != null && !opens.isEmpty()) {
                                int onIdx = opens.remove(0);
                                pairedOff[onIdx] = i;
                                pairedOn[i] = onIdx;
                            }
                        }
                    }
                }

                for(int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get(i);
                    MidiMessage msg = ev.getMessage();
                    long newTick;

                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;

                        if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0 && pairedOff[i] != -1) {
                            long offTick = t.get( pairedOff[i]).getTick();
                            newTick = Math.max(0, mirrorTick - offTick);
                        } else if ((sm.getCommand() == ShortMessage.NOTE_OFF ||
                                   (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) && pairedOn[i] != -1) {
                            long onTick = t.get(pairedOn[i]).getTick();
                            newTick = Math.max(0, mirrorTick - onTick);
                        } else {
                            newTick = Math.max(0,mirrorTick - ev.getTick());
                        }
                    } else {
                        newTick = Math.max(0,mirrorTick - ev.getTick());
                    }

                    nt.add(new MidiEvent(msg, newTick));
                }
            }

            currentSequence = n;
            sequencer.setSequence( n);
            log("Retrograd (oglindire la " + (mirrorTick / (double)res / 2) + " batai)");

        } catch (Exception ex) {
            log("Eroare: " + ex.getMessage());
        }
    }

    private static final String PI_DIGITS = "31415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";

    private void piMelody() {
        if (currentSequence == null) {
            log("Incarcati/inregistrati o melodie mai intai.");
            return;
        }

        try {
            pushHistory();

            Sequence n = new Sequence(currentSequence.getDivisionType(), currentSequence.getResolution());
            int piIdx = 0;

            for(Track t : currentSequence.getTracks()) {
                Track nt = n.createTrack();

                Map<Integer, List<Integer>> openNotes = new HashMap<>();
                int[] transposeForEvent = new int[t.size()];

                for (int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get(i);
                    MidiMessage msg = ev.getMessage();

                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        int key = sm.getData1() * 16 + sm.getChannel();

                        if(sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                            int digit = PI_DIGITS.charAt(piIdx % PI_DIGITS.length()) - '0';
                            transposeForEvent[i] = digit;
                            piIdx++;
                            openNotes.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
                        } else if (sm.getCommand() == ShortMessage.NOTE_OFF ||
                                   (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                            List<Integer> opens = openNotes.get(key);
                            if (opens != null && !opens.isEmpty()) {
                                int onIdx = opens.remove(0);
                                transposeForEvent[i] = transposeForEvent[onIdx];
                            } else {
                                transposeForEvent[i] = 0;
                            }
                        }
                    }
                }

                for (int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get( i);
                    MidiMessage msg = ev.getMessage();

                    if(msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;

                        if (sm.getCommand() == ShortMessage.NOTE_ON ||
                            sm.getCommand() == ShortMessage.NOTE_OFF) {

                            int digit = transposeForEvent[i];
                            int newNote = Math.max(0, Math.min(127, sm.getData1() + digit));
                            ShortMessage nm = new ShortMessage();
                            nm.setMessage(sm.getCommand(), sm.getChannel(), newNote, sm.getData2());
                            nt.add(new MidiEvent(nm,ev.getTick()));
                            continue;
                        }
                    }
                    nt.add(new MidiEvent(msg, ev.getTick()));
                }
            }

            currentSequence = n;
            sequencer.setSequence(n);
            log("Pi transpunere: fiecare nota +cifra Pi (3,1,4,1,5,9,2,6...)");

        } catch(Exception ex) {
            log("Eroare: " + ex.getMessage());
        }
    }

    private LearnWindow learnWindow = null;

    private void showShortcutsHelp() {
        JDialog dlg = new JDialog(this, "Scurtaturi tastatura", false);
        dlg.setSize(620, 480);
        dlg.setLocationRelativeTo( this);
        dlg.getContentPane( ).setBackground(BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        boolean midi = useMidiInput;
        String titleText = midi ? "Claviatura MIDI (LPK25)" : "Tastatura PC";

        JLabel title = new JLabel(titleText);
        title.setForeground(ACCENT_GREEN);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(12));

        String[][] entries;
        if (midi) {
            entries = new String[][] {
                {"← →", "Schimba octava claviaturii MIDI"},
                {"F1 - F12", "Selecteaza canal 1-12"},
                {"Ctrl + Z", "Undo transformare"},
                {"Ctrl + S", "Salveaza fisier MIDI"},
            };
        } else {
            entries = new String[][] {
                {"Z S X D C V G B H N J M ,", "Manual 1: do re mi fa sol la si do"},
                {"Q 2 W 3 E R 5 T 6 Y 7 U I", "Manual 2: do re mi fa sol la si do"},
                {"← →", "Octava Manual 1 jos / sus"},
                {"↑ ↓", "Octava Manual 2 sus / jos"},
                {".", "F1-F12 schimba canalul pe Manual 1"},
                {"/", "F1-F12 schimba canalul pe Manual 2"},
                {"F1 - F12", "Selecteaza canal 1-12"},
                {"Ctrl + Z", "Undo transformare"},
                {"Ctrl + S", "Salveaza fisier MIDI"},
            };
        }

        for(String[] e : entries) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

            JLabel key = new JLabel(e[0]);
            key.setForeground(ACCENT_YELLOW);
            key.setFont(new Font("Monospaced", Font.BOLD, 13));
            key.setPreferredSize(new Dimension(260, 24));

            JLabel desc = new JLabel( e[1]);
            desc.setForeground( ACCENT_YELLOW);
            desc.setFont(new Font("SansSerif", Font.PLAIN, 13));

            row.add(key, BorderLayout.WEST);
            row.add(desc, BorderLayout.CENTER);
            content.add(row);
        }

        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane( content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        dlg.add(scroll);
        dlg.setVisible(true);
    }

    private JPanel makeShortcutSection(String title, String[][] entries) {
        JPanel p = new JPanel( );
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setForeground(ACCENT);
        t.setFont(new Font("SansSerif", Font.BOLD, 13));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(6));

        for (String[] e : entries) {
            JPanel row = new JPanel( new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            JLabel key = new JLabel(e[0]);
            key.setForeground(ACCENT_YELLOW);
            key.setFont(new Font("Monospaced", Font.BOLD, 12));
            key.setPreferredSize(new Dimension(240, 20));

            JLabel desc = new JLabel(e[1]);
            desc.setForeground(TEXT_PRIMARY);
            desc.setFont(new Font("SansSerif", Font.PLAIN, 12));

            row.add(key, BorderLayout.WEST);
            row.add(desc, BorderLayout.CENTER);
            p.add(row);
        }

        return p;
    }

    private void openLearnWindow() {
        if (currentSequence == null) {
            JOptionPane.showMessageDialog(this,
                "Incarcati sau inregistrati o melodie mai intai.",
                "Invata", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (learnWindow != null && learnWindow.isDisplayable()) {
            learnWindow.toFront();
            return;
        }
        learnWindow = new LearnWindow(currentSequence);
        learnWindow.setVisible(true);
        learnWindow.toFront();
        learnWindow.requestFocus( );
    }

    class LearnWindow extends JFrame {
        private final Sequence gameSeq;
        private final List<LearnNote> notes = new ArrayList<>();
        private LearnCanvas canvas;
        private javax.swing.Timer gameTimer;
        private long startTime = 0;
        private long realStartTime = 0;
        private boolean running = false;
        private boolean paused = false;
        private long pauseAccum = 0;
        private long pauseStarted = 0;
        private int score = 0;
        private int hits = 0;
        private int misses = 0;
        private int combo = 0;
        private int maxCombo = 0;
        private double speedPx = 180;
        private int lowestNote = 127;
        private int highestNote = 0;
        private Set<Integer> pressedKeys = new HashSet<>();
        private JLabel scoreLabel, statsLabel;
        private JButton startBtn, pauseBtn2;

        static class LearnNote {
            int pitch;
            long startMs;
            long durationMs;
            boolean hit = false;
            boolean missed = false;
            LearnNote(int p, long s, long d) { pitch = p; startMs = s; durationMs = d; }
        }

        LearnWindow(Sequence seq) {
            super("Invata melodia");
            this.gameSeq = seq;
            extractNotes();
            initUI();
            setSize(1000, 650);
            setLocationRelativeTo(MidiSynthesizer.this);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }

        private void extractNotes() {
            int res = gameSeq.getResolution();
            double bpm = 120.0;
            for (Track t : gameSeq.getTracks()) {
                for (int i = 0; i < t.size(); i++) {
                    MidiMessage msg = t.get(i).getMessage();
                    if (msg instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) msg;
                        if(mm.getType() == 0x51) {
                            byte[] data = mm.getData();
                            int mpq = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                            bpm = 60_000_000.0 / mpq;
                            break;
                        }
                    }
                }
            }
            double msPerTick = 60_000.0 / (bpm * res);

            for (Track t : gameSeq.getTracks()) {
                Map<Integer, Long> open = new HashMap<>();
                Map<Integer, Integer> openChannel = new HashMap<>();
                for (int i = 0; i < t.size(); i++) {
                    MidiEvent ev = t.get(i);
                    MidiMessage msg = ev.getMessage();
                    if (msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        if(sm.getChannel() == 9) continue;
                        int pitch = sm.getData1( );
                        long ms = (long)(ev.getTick() * msPerTick);

                        if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                            open.put(pitch,ms);
                        } else if (sm.getCommand() == ShortMessage.NOTE_OFF ||
                                   (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                            Long onMs = open.remove(pitch);
                            if (onMs != null) {
                                long dur = Math.max(80, ms - onMs);
                                notes.add(new LearnNote(pitch, onMs, dur));
                                if (pitch < lowestNote) lowestNote = pitch;
                                if (pitch > highestNote) highestNote = pitch;
                            }
                        }
                    }
                }
            }

            notes.sort((a, b) -> Long.compare(a.startMs, b.startMs));

            if (!notes.isEmpty()) {
                lowestNote = Math.max(0, lowestNote - 2);
                highestNote = Math.min(127, highestNote + 2);
            } else {
                lowestNote = 60;
                highestNote = 72;
            }
        }

        private void initUI() {
            setLayout(new BorderLayout());
            getContentPane().setBackground(new Color(15, 15, 20));

            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setBackground(new Color(25, 25, 32));
            top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JPanel ctrls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            ctrls.setOpaque(false);
            startBtn = mkBtn("START", ACCENT_GREEN, 12);
            pauseBtn2 = mkBtn("PAUZA", ACCENT_YELLOW, 12);
            JButton resetBtn = mkBtn("RESET", new Color(140, 60, 60), 12);
            JButton closeBtn = mkBtn("INCHIDE", new Color(100, 100, 100), 12);
            pauseBtn2.setEnabled(false);
            startBtn.addActionListener(e -> startGame());
            pauseBtn2.addActionListener( e -> togglePause());
            resetBtn.addActionListener(e -> resetGame());
            closeBtn.addActionListener( e -> dispose());
            ctrls.add(startBtn);
            ctrls.add(pauseBtn2);
            ctrls.add(resetBtn);
            ctrls.add(closeBtn);

            JPanel speedP = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            speedP.setOpaque(false);
            JLabel speedL = new JLabel("Viteza:");
            speedL.setForeground(TEXT_DIM);
            speedL.setFont(new Font("SansSerif", Font.PLAIN, 11));
            JSlider speedSlider = new JSlider( 60, 400, 180);
            speedSlider.setOpaque(false);
            speedSlider.setFocusable(false);
            speedSlider.setPreferredSize(new Dimension(140, 22));
            JLabel speedVal = new JLabel("180 px/s");
            speedVal.setForeground(TEXT_PRIMARY);
            speedVal.setFont(new Font("Monospaced", Font.BOLD, 11));
            speedVal.setPreferredSize(new Dimension(70, 20));
            speedSlider.addChangeListener(e -> {
                speedPx = speedSlider.getValue();
                speedVal.setText(speedPx + " px/s");
            });
            speedP.add(speedL);
            speedP.add(speedSlider);
            speedP.add(speedVal);
            ctrls.add(Box.createHorizontalStrut(20));
            ctrls.add(speedP);

            scoreLabel = new JLabel("Scor: 0   Combo: 0", SwingConstants.RIGHT);
            scoreLabel.setForeground(ACCENT_GREEN);
            scoreLabel.setFont(new Font("Monospaced", Font.BOLD, 16));

            statsLabel = new JLabel("Hits: 0 | Miss: 0 | " + notes.size() + " note", SwingConstants.RIGHT);
            statsLabel.setForeground(TEXT_DIM);
            statsLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));

            JPanel rightInfo = new JPanel( new GridLayout(2, 1, 0, 2));
            rightInfo.setOpaque(false);
            rightInfo.add(scoreLabel);
            rightInfo.add(statsLabel);

            top.add(ctrls, BorderLayout.WEST);
            top.add(rightInfo, BorderLayout.EAST);

            JPanel topStack = new JPanel(new BorderLayout(0, 0));
            topStack.setBackground(new Color(25, 25, 32));
            topStack.add(top, BorderLayout.NORTH);
            topStack.add(makeColorLegend(), BorderLayout.SOUTH);
            add(topStack, BorderLayout.NORTH);

            canvas = new LearnCanvas();
            add(canvas, BorderLayout.CENTER);

            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ev -> {
                if (!isActive()) return false;
                if (ev.getID() == KeyEvent.KEY_PRESSED) {
                    handleKeyPress(ev.getKeyCode());
                } else if (ev.getID() == KeyEvent.KEY_RELEASED) {
                    handleKeyRelease(ev.getKeyCode());
                }
                return false;
            });

            gameTimer = new javax.swing.Timer(16,e -> {
                if(running && !paused) canvas.repaint();
                updateLabels();
            });
            gameTimer.start();
        }

        private JPanel makeColorLegend() {
            JPanel legend = new JPanel( new FlowLayout(FlowLayout.CENTER, 6, 4));
            legend.setBackground(new Color(20, 20, 26));
            legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 50)));
            for (int i = 0; i < 12; i++) {
                JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                item.setOpaque(false);

                JPanel swatch = new JPanel();
                swatch.setBackground(NOTE_COLORS[i]);
                swatch.setPreferredSize(new Dimension(14, 14));
                swatch.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));

                JLabel name = new JLabel(SOLFEGE_NAMES[i]);
                name.setForeground(TEXT_PRIMARY);
                name.setFont(new Font("SansSerif", Font.BOLD, 11));

                item.add(swatch);
                item.add(name);
                legend.add(item);
            }
            return legend;
        }

        private void handleKeyPress(int kc) {
            if(kc == KeyEvent.VK_SPACE) {
                if (!running) {
                    startGame();
                } else {
                    togglePause( );
                }
                return;
            }

            if (useMidiInput) {
                if(kc == KeyEvent.VK_LEFT) {
                    shiftMidiInputRange(-12);
                    return;
                }
                if (kc == KeyEvent.VK_RIGHT) {
                    shiftMidiInputRange( 12);
                    return;
                }
                if (kc == KeyEvent.VK_UP || kc == KeyEvent.VK_DOWN) return;
            } else {
                if(kc == KeyEvent.VK_LEFT) {
                    if (currentOctaveManual1 > 2) {
                        releaseAllLearnNotes();
                        currentOctaveManual1--;
                        canvas.repaint();
                    }
                    return;
                }
                if(kc == KeyEvent.VK_RIGHT) {
                    if (currentOctaveManual1 < 7) {
                        releaseAllLearnNotes();
                        currentOctaveManual1++;
                        canvas.repaint();
                    }
                    return;
                }
                if(kc == KeyEvent.VK_DOWN) {
                    if (currentOctaveManual2 > 2) {
                        releaseAllLearnNotes();
                        currentOctaveManual2--;
                        canvas.repaint();
                    }
                    return;
                }
                if(kc == KeyEvent.VK_UP) {
                    if(currentOctaveManual2 < 7) {
                        releaseAllLearnNotes();
                        currentOctaveManual2++;
                        canvas.repaint();
                    }
                    return;
                }
            }

            if (useMidiInput) return;

            if(pressedKeys.contains(kc)) return;
            pressedKeys.add(kc);

            int note = keyToNote(kc);
            if(note < 0) return;

            if (midiChannels[currentChannel] != null) {
                midiChannels[currentChannel].noteOn(note, 100);
            }
            canvas.repaint();

            if (!running || paused) return;

            long now = currentGameTime();
            LearnNote hitNote = null;
            long bestDiff = 250;

            for(LearnNote n : notes) {
                if (n.hit || n.missed) continue;
                if(n.pitch != note) continue;
                long diff = Math.abs(n.startMs - now);
                if(diff < bestDiff) {
                    bestDiff = diff;
                    hitNote = n;
                }
            }

            if(hitNote != null) {
                hitNote.hit = true;
                hits++;
                combo++;
                if (combo > maxCombo) maxCombo = combo;
                int points = 100 + (combo * 10);
                if(bestDiff < 50) points += 50;
                score += points;
            } else {
                combo = 0;
            }
        }

        private void releaseAllLearnNotes() {
            for (int kc : new HashSet<>(pressedKeys)) {
                int note = keyToNote(kc);
                if (note >= 0 && midiChannels[currentChannel] != null) {
                    midiChannels[currentChannel].noteOff(note);
                }
            }
            pressedKeys.clear();
        }

        private Set<Integer> externalActiveNotes = new HashSet<>();

        public void externalNoteOn(int note, int vel) {
            externalActiveNotes.add(note);
            canvas.repaint();

            if (!running || paused) return;

            long now = currentGameTime();
            LearnNote hitNote = null;
            long bestDiff = 250;

            for (LearnNote n : notes) {
                if (n.hit || n.missed) continue;
                if (n.pitch != note) continue;
                long diff = Math.abs(n.startMs - now);
                if(diff < bestDiff) {
                    bestDiff = diff;
                    hitNote = n;
                }
            }

            if (hitNote != null) {
                hitNote.hit = true;
                hits++;
                combo++;
                if(combo > maxCombo) maxCombo = combo;
                int points = 100 + (combo * 10);
                if (bestDiff < 50) points += 50;
                score += points;
            } else {
                combo = 0;
            }
        }

        public void externalNoteOff(int note) {
            externalActiveNotes.remove(note);
            canvas.repaint();
        }

        private void handleKeyRelease(int kc) {
            pressedKeys.remove(kc);
            int note = keyToNote(kc);
            if(note < 0) return;
            if (midiChannels[currentChannel] != null) {
                midiChannels[currentChannel].noteOff(note);
            }
            canvas.repaint();
        }

        private int keyToNote(int kc) {
            if(manual1Map.containsKey(kc)) {
                return (currentOctaveManual1 + 1) * 12 + manual1Map.get(kc);
            }
            if(manual2Map.containsKey(kc)) {
                return (currentOctaveManual2 + 1) * 12 + manual2Map.get(kc);
            }
            return -1;
        }

        private long currentGameTime() {
            if (!running) return 0;
            if(paused) return pauseStarted - startTime - pauseAccum;
            return System.currentTimeMillis( ) - startTime - pauseAccum;
        }

        private void startGame() {
            if (running) return;
            int canvasH = canvas.getHeight();
            long noteFallTime = (long)(canvasH * 1000.0 / speedPx);
            realStartTime = System.currentTimeMillis();
            startTime = realStartTime + 3000 + noteFallTime;
            pauseAccum = 0;
            running = true;
            paused = false;
            for (LearnNote n : notes) { n.hit = false; n.missed = false; }
            score = 0;
            hits = 0;
            misses = 0;
            combo = 0;
            startBtn.setEnabled(false);
            pauseBtn2.setEnabled(true);
        }

        private void togglePause( ) {
            if (!running) return;
            if(paused) {
                pauseAccum += System.currentTimeMillis() - pauseStarted;
                paused = false;
                pauseBtn2.setText("PAUZA");
            } else {
                pauseStarted = System.currentTimeMillis( );
                paused = true;
                pauseBtn2.setText("CONTINUA");
            }
        }

        private void resetGame() {
            running = false;
            paused = false;
            for (LearnNote n : notes) { n.hit = false; n.missed = false; }
            score = 0;
            hits = 0;
            misses = 0;
            combo = 0;
            maxCombo = 0;
            startBtn.setEnabled(true);
            pauseBtn2.setEnabled(false);
            pauseBtn2.setText("PAUZA");
            canvas.repaint();
        }

        private void updateLabels( ) {
            scoreLabel.setText("Scor: " + score + "   Combo: " + combo);
            statsLabel.setText("Hits: " + hits + " | Miss: " + misses +
                " | " + notes.size() + " note | Max combo: " + maxCombo);
        }

        @Override public void dispose() {
            if (gameTimer != null) gameTimer.stop();
            running = false;
            super.dispose();
        }

        class LearnCanvas extends JPanel {
            LearnCanvas() { setBackground(new Color(15, 15, 20)); }

            int whiteCount() {
                int c = 0;
                for( int n = START_NOTE; n <= END_NOTE; n++) if (!isBlack(n)) c++;
                return c;
            }

            int wkw() { return Math.max(10, getWidth() / whiteCount()); }
            int bkw() { return (int)(wkw() * 0.58); }
            int wkh() { return 140; }
            int bkh() { return (int)(wkh() * 0.62); }

            int noteX(int note) {
                int W = wkw();
                int x = 0;
                for (int n = START_NOTE; n <= note; n++) {
                    if (!isBlack(n)) {
                        if(n == note) return x + W / 2;
                        x += W;
                    } else {
                        if(n == note) return x;
                    }
                }
                return x + W / 2;
            }

            int noteWidth(int note) {
                return isBlack(note) ? bkw() : wkw() - 4;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 =(Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int keyboardH = wkh() + 18;
                int kbY = h - keyboardH;
                int playZoneY = kbY - 10;

                g2.setColor(new Color(15, 15, 20));
                g2.fillRect( 0, 0, w, kbY);

                for( int n = START_NOTE; n <= END_NOTE; n++) {
                    if (n % 12 == 0) {
                        int x = noteX(n) - wkw() / 2;
                        g2.setColor(new Color(30, 30, 40));
                        g2.drawLine(x, 0, x, kbY);
                    }
                }

                g2.setColor(new Color(255, 220, 80, 180));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(0, playZoneY, w, playZoneY);
                g2.setStroke(new BasicStroke(1f));

                g2.setColor(new Color(255, 220, 80, 25));
                g2.fillRect(0, playZoneY - 25, w, 50);

                long now = currentGameTime();
                if(!running) now = -3000;

                for (LearnNote ln : notes) {
                    if(ln.pitch < START_NOTE || ln.pitch > END_NOTE) continue;

                    long timeToHit = ln.startMs - now;
                    long timeFromEnd =(ln.startMs + ln.durationMs) - now;

                    int yBottom = playZoneY - (int)(timeToHit * speedPx / 1000.0);
                    int yTop = playZoneY - (int)(timeFromEnd * speedPx / 1000.0);

                    if (yBottom < 0 || yTop > playZoneY + 60) continue;

                    if (!ln.hit && !ln.missed && yBottom > playZoneY + 30 && running) {
                        ln.missed = true;
                        misses++;
                        combo = 0;
                    }

                    int xCenter = noteX(ln.pitch);
                    int barW = noteWidth(ln.pitch);
                    int x = xCenter - barW / 2;

                    Color col;
                    if (ln.hit) col = new Color(80, 220, 120);
                    else if (ln.missed) col = new Color(220, 80, 80);
                    else col = NOTE_COLORS[ln.pitch % 12];

                    int barH = Math.max( 8, yBottom - yTop);
                    g2.setPaint(new GradientPaint(x, yTop, col, x, yBottom, col.darker()));
                    g2.fillRoundRect(x, yTop, barW, barH, 6, 6);

                    g2.setColor(new Color(255, 255, 255, 100));
                    g2.drawRoundRect(x, yTop, barW, barH, 6, 6);

                    if (barH > 22) {
                        g2.setColor(new Color(255, 255, 255, 220));
                        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                        String lbl = SOLFEGE_NAMES[ln.pitch % 12];
                        g2.drawString(lbl, x + 3, yBottom - 6);
                    }
                }

                drawFullPiano(g2, kbY, keyboardH);

                if (running) {
                    long elapsed = System.currentTimeMillis( ) - realStartTime - pauseAccum;
                    if(paused) elapsed = pauseStarted - realStartTime - pauseAccum;
                    if(elapsed >= 0 && elapsed < 3000) {
                        int secondsLeft = 3 - (int)(elapsed / 1000);
                        String msg = String.valueOf(secondsLeft);
                        g2.setFont(new Font("SansSerif", Font.BOLD, 140));
                        FontMetrics fm = g2.getFontMetrics();
                        int msgW = fm.stringWidth(msg);
                        g2.setColor( new Color(255, 255, 255, 220));
                        g2.drawString(msg,w / 2 - msgW / 2, kbY / 2 + 40);
                    }
                } else {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                    FontMetrics fm = g2.getFontMetrics();
                    String msg = "Apasa START pentru a incepe";
                    int msgW = fm.stringWidth(msg);
                    g2.setColor(new Color(255, 255, 255, 170));
                    g2.drawString(msg, w / 2 - msgW / 2, kbY / 2);

                    g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    String msg2 = "Sageti stanga/dreapta = octava M1 | sus/jos = octava M2";
                    int msg2W = g2.getFontMetrics().stringWidth(msg2);
                    g2.setColor( new Color(180, 180, 190, 180));
                    g2.drawString(msg2,w / 2 - msg2W / 2, kbY / 2 + 28);
                }
            }

            private void drawFullPiano(Graphics2D g2, int kbY, int keyboardH) {
                int W = wkw();
                int BW = bkw();
                int H = wkh();
                int BH = bkh();

                Set<Integer> active = new HashSet<>();
                for (int kc : pressedKeys) {
                    int nn = keyToNote( kc);
                    if (nn >= 0) active.add(nn);
                }
                active.addAll(externalActiveNotes);

                int x = 0;
                for(int note = START_NOTE; note <= END_NOTE; note++) {
                    if(!isBlack(note)) {
                        boolean a = active.contains( note);
                        boolean inM1,inM2;
                        if (useMidiInput) {
                            inM1 = note >= midiInputLowestNote && note <= midiInputHighestNote;
                            inM2 = false;
                        } else {
                            inM1 = note >= (currentOctaveManual1 + 1) * 12 && note <= (currentOctaveManual1 + 1) * 12 + 12;
                            inM2 = note >= (currentOctaveManual2 + 1) * 12 && note <= (currentOctaveManual2 + 1) * 12 + 12;
                        }

                        if (a) {
                            g2.setPaint(new GradientPaint(x,kbY, CH_COLORS[currentChannel],
                                x, kbY + H, CH_COLORS[currentChannel].darker( )));
                        } else if (inM1 && inM2) {
                            g2.setPaint(new GradientPaint(x,kbY, new Color(210, 240, 210),
                                x, kbY + H, new Color(185, 215, 185)));
                        } else if(inM1) {
                            g2.setPaint(new GradientPaint(x, kbY, new Color(210, 225, 245),
                                x, kbY + H, new Color(185, 200, 225)));
                        } else if (inM2) {
                            g2.setPaint(new GradientPaint(x, kbY, new Color(245, 225, 210),
                                x, kbY + H, new Color(225, 200, 185)));
                        } else {
                            g2.setPaint(new GradientPaint(x, kbY, new Color(250, 250, 252),
                                x, kbY + H, new Color(220, 220, 225)));
                        }
                        g2.fillRoundRect(x + 1, kbY, W - 2, H, 2, 4);
                        g2.setColor(new Color(170, 170, 180));
                        g2.drawRoundRect(x + 1, kbY, W - 2, H, 2, 4);

                        if(note % 12 == 0) {
                            g2.setColor(new Color(140, 140, 150));
                            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                            g2.drawString("Do" + (note / 12 - 1), x + 3, kbY + H - 3);
                        }
                        x += W;
                    }
                }

                x = 0;
                for (int note = START_NOTE; note <= END_NOTE; note++) {
                    if(!isBlack(note)) {
                        if(note + 1 <= END_NOTE && isBlack(note + 1)) {
                            int bx = x + W - BW / 2;
                            boolean a = active.contains(note + 1);
                            boolean bInM1, bInM2;
                            if (useMidiInput) {
                                bInM1 = (note + 1) >= midiInputLowestNote && (note + 1) <= midiInputHighestNote;
                                bInM2 = false;
                            } else {
                                bInM1 = (note + 1) >= (currentOctaveManual1 + 1) * 12 && (note + 1) <= (currentOctaveManual1 + 1) * 12 + 12;
                                bInM2 = (note + 1) >= (currentOctaveManual2 + 1) * 12 && (note + 1) <= (currentOctaveManual2 + 1) * 12 + 12;
                            }

                            if (a) {
                                g2.setPaint(new GradientPaint(bx, kbY, CH_COLORS[currentChannel].darker(),
                                    bx, kbY + BH, CH_COLORS[currentChannel].darker().darker()));
                            } else if(bInM1 && bInM2) {
                                g2.setPaint( new GradientPaint(bx, kbY, new Color(55, 70, 55),
                                    bx,kbY + BH, new Color(30, 42, 30)));
                            } else if (bInM1) {
                                g2.setPaint( new GradientPaint(bx, kbY, new Color(50, 55, 70),
                                    bx, kbY + BH, new Color(28, 32, 45)));
                            } else if (bInM2) {
                                g2.setPaint(new GradientPaint(bx, kbY, new Color(70, 55, 50),
                                    bx,kbY + BH, new Color(45, 32, 28)));
                            } else {
                                g2.setPaint(new GradientPaint(bx, kbY, new Color(40, 40, 45),
                                    bx,kbY + BH, new Color(20, 20, 22)));
                            }
                            g2.fillRoundRect(bx, kbY, BW, BH, 2, 3);
                            g2.setColor(new Color(15, 15, 18));
                            g2.drawRoundRect(bx,kbY, BW, BH, 2, 3);
                        }
                        x += W;
                    }
                }
            }
        }
    }

    private void saveMidiFile() {
        if (currentSequence == null) {
            log("Nimic de salvat.");
            return;
        }

        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter("MIDI (*.mid)", "mid"));
        ch.setSelectedFile(new File("output.mid"));

        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = ch.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".mid")) {
                    f = new File(f.getPath() + ".mid");
                }
                MidiSystem.write(currentSequence, 1, f);
                log("Salvat: " + f.getAbsolutePath());

            } catch (IOException ex) {
                log("Eroare: " + ex.getMessage());
            }
        }
    }

    private boolean isBlack(int n){int m=n%12;return m==1||m==3||m==6||m==8||m==10;}

    private String formatTime(long s){return String.format("%02d:%02d",s/60,s%60);}

    private int calcPivot(Sequence seq){long sum=0;int cnt=0;for(Track t:seq.getTracks()) for(int i=0;i<t.size();i++){MidiMessage msg=t.get(i).getMessage();
        if(msg instanceof ShortMessage){ShortMessage sm=(ShortMessage)msg;if(sm.getCommand()==ShortMessage.NOTE_ON&&sm.getData2()>0){sum+=sm.getData1();cnt++;}}}return cnt>0?(int)(sum/cnt):60;}

    private void log(String msg){if(transformLogArea!=null) SwingUtilities.invokeLater(()->{transformLogArea.append(msg+"\n");transformLogArea.setCaretPosition(transformLogArea.getDocument().getLength());});}

    public static void main(String[] args){
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("TabbedPane.contentAreaColor",BG_PANEL);
            UIManager.put("TabbedPane.selected",BG_HOVER);
            UIManager.put("TabbedPane.background",BG_PANEL);
            UIManager.put("TabbedPane.shadow",BORDER_COLOR);
        }catch(Exception e){e.printStackTrace();
        }
        SwingUtilities.invokeLater(MidiSynthesizer::new);
        }
}
