import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class PacmanGame extends JFrame implements KeyListener {

    GamePanel gamePanel = new GamePanel();
    MenuPanel MP;
    Splash LS;
    public Graphics g;
    public boolean running;
    public boolean dead = false;
    public boolean paused = false;
    public boolean animate = false;
    public long dt = 0L;
    public long timePreviousFrame = System.currentTimeMillis();
    public long timeCurrentFrame  = System.currentTimeMillis();
    public long timeStarted       = System.currentTimeMillis();
    public long timeInterval      = 40L; //25 fps
    public long SplashFrame;
    public Room currentRoom;
    public Menu currMenu;
    public LevelSplash currSplash;
    public boolean start;
    public int level = 1;
    public int score = 0;
    public int highscore = 0;
    public int lives = 3;

    public static final int WIDTH = 336;
    public static final int HEIGHT= 480;
    public static final boolean DEBUG = false;

    public PacmanGame() {
        this.menuInit();
    }

    //Start Screen before initialization
    public void menuInit() {
        AudioPlayer.init();
        AudioPlayer.stopAll();
        AudioPlayer.BEGINNING.play();
        running = true;
        start = true;
        dead = false;
        level =1;
        score = 0;
        MP = new MenuPanel();
        JFrame menu = this;
        menu.setMinimumSize(new Dimension(PacmanGame.WIDTH, PacmanGame.HEIGHT));
        menu.setTitle("Pacman");
        g = menu.getContentPane().getGraphics();
        menu.getContentPane().add(MP);
        currMenu = new Menu();
        MP.menu = currMenu;
        menu.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent keyEvent) {

                }
                @Override
                public void keyPressed(KeyEvent e) {
                    int code = e.getKeyCode();
                    if(KeyEvent.VK_ENTER == code) start = false;
                }
                @Override
                public void keyReleased(KeyEvent keyEvent) {

                }
            });
        menu.setFocusable(true);
        menu.setVisible(true);
        while (start){
            menuLoop();
        }
        menu.getContentPane().remove(MP);
        initGame();
    }

    //Game Initialization - Place something here if you only want it to happen globally when the game is started.
    public void initGame() {
        JFrame frame = this;
        //  12x12 pixel square on the screen. Pac-Man’s screen resolution is 336 x 432 (plus five rows for status totaling 60),
        //      so this gives us a total board size of 28 x 30 tiles,
        frame.setMinimumSize(new Dimension(PacmanGame.WIDTH , PacmanGame.HEIGHT)); //Fixed resolution - Don't change, timing and graphics rely on this resolution.
        frame.setTitle("Pacman");
        g = frame.getContentPane().getGraphics();
        frame.getContentPane().add(gamePanel);
        currentRoom = new Room(level,score,lives,highscore); //Start at level one
        gamePanel.room = currentRoom;
        frame.addKeyListener(this);
        frame.addKeyListener(currentRoom);
        frame.pack();
        frame.setVisible(true);
        while (running) {
            gameLoop();
            if(currentRoom.score > highscore){
                highscore = currentRoom.score;
                currentRoom.highscore = highscore;
            }
            if (currentRoom.numLives <= 0 && !currentRoom.pacman.dead){
                running = false;
                dead = true;
                highscore = currentRoom.highscore;
            }
            else if(currentRoom.dots.isEmpty()){
                level++;
                score = currentRoom.score;
                lives = currentRoom.numLives;
                running = false;
            }
        }
        //Final frame update to ensure game over is shown
        if (dead) {
            this.update(dt);
            this.repaint();
            //makes sure lives resets to three upon a game over
            lives = 3;
            long test = System.currentTimeMillis();
            while (System.currentTimeMillis() < test + 1500) {
                //do nothing
            }
            frame.getContentPane().remove(gamePanel);
            menuInit();

        }
        else{
            frame.getContentPane().remove(gamePanel);
            levelSplash();
        }
    }

    private void levelSplash() {
        //create and draw Level Splash Screen
        running = true;
        LS = new Splash();
        JFrame ls = this;
        ls.setMinimumSize(new Dimension(PacmanGame.WIDTH, PacmanGame.HEIGHT)); //Fixed resolution - Don't change, timing and graphics rely on this resolution.
        ls.setTitle("Pacman");
        g = ls.getContentPane().getGraphics();
        ls.getContentPane().add(LS);
        currSplash = new LevelSplash(level);
        LS.Spl = currSplash;
        ls.pack();
        ls.setVisible(true);
        AudioPlayer.stopAll();
        AudioPlayer.INTERMISSION.play();
        animate = true;
        SplashFrame = System.currentTimeMillis();
        while (animate) {
            SplashLoop();
        }
        ls.getContentPane().remove(LS);
        initGame();
    }

    private void SplashLoop() {
        this.timeCurrentFrame = System.currentTimeMillis();
        this.dt = this.timeCurrentFrame - this.timePreviousFrame;
        this.timePreviousFrame = this.timeCurrentFrame;
        long timeComputationStart = System.currentTimeMillis();
        //UPDATE AND DRAW
        this.update(dt);
        this.repaint();
        //sleep
        try {
            long timeComputationEnd = System.currentTimeMillis();
            long timeComputationTaken = timeComputationEnd - timeComputationStart;
            long timeToSleep = this.timeInterval - timeComputationTaken;
            Thread.sleep(timeToSleep);
        } catch (Exception e) {
            System.err.println("ERROR: Could not sleep main thread.");
            e.printStackTrace();
        }
        //see if Animation is done
        if(System.currentTimeMillis()-SplashFrame > 5000) animate = false;
    }

    public void menuLoop() {
        KeyListener tester = new KeyListener() {
                @Override
                public void keyTyped(KeyEvent keyEvent) {

                }

                @Override
                public void keyPressed(KeyEvent e) {
                    int code = e.getKeyCode();
                    if(KeyEvent.VK_ENTER == code){start = false;}
                }

                @Override
                public void keyReleased(KeyEvent keyEvent) {

                }
            };
    }


    public void gameLoop() {
        //CLOCK
        this.timeCurrentFrame = System.currentTimeMillis();
        this.dt = this.timeCurrentFrame - this.timePreviousFrame;
        this.timePreviousFrame = this.timeCurrentFrame;
        long timeComputationStart = System.currentTimeMillis();
        //UPDATE AND DRAW
        if (!paused) this.update(dt);
        this.repaint();
        //this.paintComponent(g);
        //SLEEP IF NEEDED
        try {
            long timeComputationEnd = System.currentTimeMillis();
            long timeComputationTaken = timeComputationEnd - timeComputationStart;
            long timeToSleep = this.timeInterval - timeComputationTaken;
            if (timeToSleep >= 0) {
                Thread.sleep(timeToSleep);
            } else {
                Thread.sleep(0);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Could not sleep main thread.");
            e.printStackTrace();
        }
    } 

    //Game Release - Do before game closes
    public void releaseGame() {

    }

    //UPDATE LOOP
    public void update(long dt) {

        if(animate)currSplash.update(dt);
        else currentRoom.update(dt);
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_SPACE) {
            if (paused) {
                paused = false;
                currentRoom.paused = false;
            }
            else if (!paused) {
                paused = true;
                currentRoom.paused = true;
            }
        }
        //tester for level animation
        if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE){
            running = false;
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }

    public class GamePanel extends JPanel {
        public Room room;
        //DRAW LOOP
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            currentRoom.draw(g2);
        }
    }

    public class MenuPanel extends JPanel {
        public Menu menu;
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            menu.draw(g2);
        }
    }

    public class Splash extends JPanel {
        public LevelSplash Spl;
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            Spl.draw(g2);
        }
    }

    public static void main(String[] args) {
        PacmanGame game = new PacmanGame();
        System.out.println(game.getTitle());
    }

}
