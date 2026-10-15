// CannonView.java
// Exibe e controla o Cannon Game (Figs. 6.19 a 6.34)
package com.deitel.cannongame;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView
   implements SurfaceHolder.Callback {

   private static final String TAG = "CannonView"; // para registrar erros

   // constantes da jogabilidade
   public static final int MISS_PENALTY = 2; // segundos descontados ao errar
   public static final int HIT_REWARD = 3; // segundos somados ao acertar

   // constantes do canhão
   public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
   public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
   public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

   // constantes da bala de canhão
   public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
   public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

   // constantes dos alvos
   public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
   public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
   public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
   public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
   public static final double TARGET_PIECES = 9;
   public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
   public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

   // constantes do bloqueador
   public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
   public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
   public static final double BLOCKER_X_PERCENT = 1.0 / 2;
   public static final double BLOCKER_SPEED_PERCENT = 1.0;

   // tamanho do texto: 1/18 da altura da tela
   public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

   private CannonThread cannonThread; // controla o loop do jogo
   private Activity activity; // para exibir o diálogo de fim de jogo na thread da GUI
   private boolean dialogIsDisplayed = false;

   // objetos do jogo
   private Cannon cannon;
   private Blocker blocker;
   private ArrayList<Target> targets;

   // variáveis de dimensão
   private int screenWidth;
   private int screenHeight;

   // variáveis do loop do jogo e das estatísticas
   private boolean gameOver; // o jogo terminou?
   private double timeLeft; // tempo restante em segundos
   private int shotsFired; // disparos feitos pelo usuário
   private double totalElapsedTime; // segundos decorridos

   // constantes e variáveis para gerenciar os sons
   public static final int TARGET_SOUND_ID = 0;
   public static final int CANNON_SOUND_ID = 1;
   public static final int BLOCKER_SOUND_ID = 2;
   private SoundPool soundPool; // toca os efeitos sonoros
   private SparseIntArray soundMap; // mapeia IDs para o SoundPool

   // variáveis Paint usadas para desenhar cada item na tela
   private Paint textPaint; // Paint usado para desenhar o texto
   private Paint backgroundPaint; // Paint usado para limpar a área de desenho

   // construtor
   public CannonView(Context context, AttributeSet attrs) {
      super(context, attrs); // chama o construtor da superclasse
      activity = (Activity) context; // guarda referência à MainActivity

      // registra o listener SurfaceHolder.Callback
      getHolder().addCallback(this);

      // configura os atributos de áudio para áudio de jogo
      AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
      attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

      // inicializa o SoundPool para tocar os três efeitos sonoros do app
      SoundPool.Builder builder = new SoundPool.Builder();
      builder.setMaxStreams(1);
      builder.setAudioAttributes(attrBuilder.build());
      soundPool = builder.build();

      // cria o mapa de sons e pré-carrega os sons
      soundMap = new SparseIntArray(3); // cria novo SparseIntArray
      soundMap.put(TARGET_SOUND_ID,
         soundPool.load(context, R.raw.target_hit, 1));
      soundMap.put(CANNON_SOUND_ID,
         soundPool.load(context, R.raw.cannon_fire, 1));
      soundMap.put(BLOCKER_SOUND_ID,
         soundPool.load(context, R.raw.blocker_hit, 1));

      textPaint = new Paint();
      backgroundPaint = new Paint();
      backgroundPaint.setColor(Color.WHITE);
   }

   // chamado quando o tamanho do SurfaceView muda,
   // por exemplo, quando ele é adicionado à hierarquia de Views
   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);

      screenWidth = w; // guarda a largura do CannonView
      screenHeight = h; // guarda a altura do CannonView

      // configura as propriedades do texto
      textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
      textPaint.setAntiAlias(true); // suaviza o texto
   }

   // obtém a largura da tela do jogo
   public int getScreenWidth() {
      return screenWidth;
   }

   // obtém a altura da tela do jogo
   public int getScreenHeight() {
      return screenHeight;
   }

   // toca o som com o soundId informado no soundMap
   public void playSound(int soundId) {
      if (soundPool != null) // o SoundPool pode já ter sido liberado
         soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
   }

   // reinicia todos os elementos da tela e começa um novo jogo
   public void newGame() {
      // constrói um novo canhão
      cannon = new Cannon(this,
         (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
         (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
         (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

      Random random = new Random(); // para determinar velocidades aleatórias
      targets = new ArrayList<>(); // constrói uma nova lista de alvos

      // inicializa targetX para o primeiro alvo a partir da esquerda
      int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);

      // calcula a coordenada Y dos alvos
      int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) *
         screenHeight);

      // adiciona TARGET_PIECES alvos à lista
      for (int n = 0; n < TARGET_PIECES; n++) {

         // determina uma velocidade aleatória entre os valores mínimo e
         // máximo para o alvo n
         double velocity = screenHeight * (random.nextDouble() *
            (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) +
            TARGET_MIN_SPEED_PERCENT);

         // alterna as cores dos alvos entre escura e clara
         int color = (n % 2 == 0) ?
            getResources().getColor(R.color.dark,
               getContext().getTheme()) :
            getResources().getColor(R.color.light,
               getContext().getTheme());

         velocity *= -1; // inverte a velocidade inicial para o próximo alvo

         // cria e adiciona um novo alvo à lista
         targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
            (int) (TARGET_WIDTH_PERCENT * screenWidth),
            (int) (TARGET_LENGTH_PERCENT * screenHeight),
            (int) velocity));

         // aumenta a coordenada x para posicionar o próximo alvo mais à
         // direita
         targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) *
            screenWidth;
      }

      // cria um novo bloqueador
      blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
         (int) (BLOCKER_X_PERCENT * screenWidth),
         (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
         (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
         (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
         (float) (BLOCKER_SPEED_PERCENT * screenHeight));

      timeLeft = 10; // começa a contagem regressiva em 10 segundos

      shotsFired = 0; // define o número inicial de disparos
      totalElapsedTime = 0.0; // zera o tempo decorrido

      if (gameOver) { // começa um novo jogo depois que o anterior terminou
         gameOver = false; // o jogo não terminou
         cannonThread = new CannonThread(getHolder()); // cria a thread
         cannonThread.start(); // inicia a thread do loop do jogo
      }

      hideSystemBars();
   }

   // chamado repetidamente pela CannonThread para atualizar os elementos
   private void updatePositions(double elapsedTimeMS) {
      double interval = elapsedTimeMS / 1000.0; // converte para segundos

      // atualiza a posição da bala se ela estiver na tela
      if (cannon.getCannonball() != null)
         cannon.getCannonball().update(interval);

      blocker.update(interval); // atualiza a posição do bloqueador

      for (GameElement target : targets)
         target.update(interval); // atualiza a posição do alvo

      timeLeft -= interval; // subtrai do tempo restante

      // se o cronômetro chegou a zero
      if (timeLeft <= 0) {
         timeLeft = 0.0;
         gameOver = true; // o jogo terminou
         cannonThread.setRunning(false); // encerra a thread
         // >>> PARTE 2: mostrar o diálogo de derrota (passo 2)
      }

      // se todos os alvos foram atingidos
      if (targets.isEmpty()) {
         cannonThread.setRunning(false); // encerra a thread
         // >>> PARTE 2: mostrar o diálogo de vitória (passo 2)
         gameOver = true;
      }
   }

   // >>> PARTE 2: métodos alignAndFireCannonball, showGameOverDialog e resetGameFromDialog (passo 3)

   // desenha o jogo no Canvas informado
   public void drawGameElements(Canvas canvas) {
      // limpa o fundo
      canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
         backgroundPaint);

      // exibe o tempo restante
      canvas.drawText(getResources().getString(
         R.string.time_remaining_format, timeLeft), 50, 100, textPaint);

      cannon.draw(canvas); // desenha o canhão

      // desenha os GameElements
      if (cannon.getCannonball() != null &&
         cannon.getCannonball().isOnScreen())
         cannon.getCannonball().draw(canvas);

      blocker.draw(canvas); // desenha o bloqueador

      // desenha todos os alvos
      for (GameElement target : targets)
         target.draw(canvas);
   }

   // >>> PARTE 2: método testForCollisions (passo 4)

   // para o jogo: chamado pelo método onPause do MainActivityFragment
   public void stopGame() {
      if (cannonThread != null)
         cannonThread.setRunning(false); // pede para a thread terminar
   }

   // libera recursos: chamado pelo método onDestroy do MainActivityFragment
   public void releaseResources() {
      if (soundPool != null) {
         soundPool.release(); // libera os recursos usados pelo SoundPool
         soundPool = null;
      }
   }

   // chamado quando a superfície muda de tamanho
   @Override
   public void surfaceChanged(SurfaceHolder holder, int format,
      int width, int height) { }

   // chamado quando a superfície é criada pela primeira vez
   @Override
   public void surfaceCreated(SurfaceHolder holder) {
      if (!dialogIsDisplayed) {
         newGame(); // configura e inicia um novo jogo
         cannonThread = new CannonThread(holder); // cria a thread
         cannonThread.setRunning(true); // inicia o jogo
         cannonThread.start(); // inicia a thread do loop do jogo
      }
   }

   // chamado quando a superfície é destruída
   @Override
   public void surfaceDestroyed(SurfaceHolder holder) {
      if (cannonThread == null)
         return;

      // garante que a thread termine corretamente
      boolean retry = true;
      cannonThread.setRunning(false); // encerra a cannonThread

      while (retry) {
         try {
            cannonThread.join(); // espera a cannonThread terminar
            retry = false;
         }
         catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
         }
      }
   }

   // >>> PARTE 2: método onTouchEvent (passo 6)

   // subclasse de Thread que controla o loop do jogo
   private class CannonThread extends Thread {
      private SurfaceHolder surfaceHolder; // para manipular o canvas
      private volatile boolean threadIsRunning = true; // executando por padrão

      // inicializa o surface holder
      public CannonThread(SurfaceHolder holder) {
         surfaceHolder = holder;
         setName("CannonThread");
      }

      // altera o estado de execução
      public void setRunning(boolean running) {
         threadIsRunning = running;
      }

      // controla o loop do jogo
      @Override
      public void run() {
         Canvas canvas = null; // usado para desenhar
         long previousFrameTime = System.currentTimeMillis();

         while (threadIsRunning) {
            try {
               // obtém o Canvas para desenho exclusivo a partir desta thread
               canvas = surfaceHolder.lockCanvas(null);

               // a superfície pode não estar disponível neste quadro
               if (canvas == null)
                  continue;

               // bloqueia o surfaceHolder para desenhar
               synchronized(surfaceHolder) {
                  long currentTime = System.currentTimeMillis();
                  double elapsedTimeMS = currentTime - previousFrameTime;
                  totalElapsedTime += elapsedTimeMS / 1000.0;
                  updatePositions(elapsedTimeMS); // atualiza o estado do jogo
                  // >>> PARTE 2: chamar testForCollisions no loop do jogo (passo 5)
                  drawGameElements(canvas); // desenha usando o canvas
                  previousFrameTime = currentTime; // atualiza o tempo anterior
               }
            }
            finally {
               // exibe o conteúdo do canvas no CannonView
               // e permite que outras threads usem o Canvas
               if (canvas != null)
                  surfaceHolder.unlockCanvasAndPost(canvas);
               canvas = null;
            }
         }
      }
   }

   // oculta as barras do sistema e a barra do app (modo imersivo)
   @SuppressWarnings("deprecation") // API do livro (Seção 6.13.16)
   private void hideSystemBars() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
         setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE);
   }

   // mostra as barras do sistema e a barra do app
   @SuppressWarnings("deprecation") // API do livro (Seção 6.13.16)
   private void showSystemBars() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
         setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
   }

   // >>> PARTE 2: classe GameResultDialogFragment (passo 7)
}
