// CannonView.java
// Exibe e controla o Cannon Game (Figs. 6.19 a 6.34)
package com.deitel.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

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
         showGameOverDialog(R.string.lose); // tempo esgotado: derrota
      }

      // se todos os alvos foram atingidos
      if (targets.isEmpty()) {
         cannonThread.setRunning(false); // encerra a thread
         showGameOverDialog(R.string.win); // todos os alvos destruídos: vitória
         gameOver = true;
      }
   }

   // mira o cano na direção do toque e dispara uma bala,
   // desde que não exista outra bala na tela
   public void alignAndFireCannonball(MotionEvent event) {
      // ponto da tela onde o usuário tocou
      Point touchPoint = new Point((int) event.getX(),
              (int) event.getY());

      // o canhão fica na borda esquerda, no meio da altura da tela;
      // centerMinusY é a distância vertical entre o canhão e o toque
      // (positiva se o toque está acima do canhão, negativa se está abaixo)
      double centerMinusY = (screenHeight / 2 - touchPoint.y);

      double angle = 0; // inicializa o ângulo como 0

      // atan2 recebe a distância horizontal (touchPoint.x) e a vertical
      // (centerMinusY) e devolve o ângulo, em radianos, da reta que liga
      // o canhão ao toque. Neste jogo, 0 aponta para cima e PI/2 aponta
      // para a direita, por isso x vem como primeiro argumento.
      angle = Math.atan2(touchPoint.x, centerMinusY);

      cannon.align(angle); // gira o cano para o ângulo calculado

      // só permite um disparo se não houver bala ou se a anterior
      // já saiu da tela (no máximo uma bala por vez)
      if (cannon.getCannonball() == null ||
              !cannon.getCannonball().isOnScreen()) {
         cannon.fireCannonball(); // cria a bala e toca o som do tiro
         ++shotsFired;
      }
   }

   // exibe o diálogo de fim de jogo com o resultado, o número de
   // disparos e o tempo total da partida
   private void showGameOverDialog(final int messageId) {
      // O livro cria o DialogFragment como classe anônima. A partir do
      // Android 8.0, o FragmentManager exige uma classe pública e estática
      // (senão lança IllegalStateException e o app fecha), por isso usamos
      // GameResultDialogFragment, que recebe os dados da partida.
      final DialogFragment gameResult = GameResultDialogFragment.newInstance(
              messageId, shotsFired, totalElapsedTime);

      // Este método é chamado pela CannonThread, mas no Android só a
      // thread principal (UI thread) pode alterar a interface. Por isso
      // o diálogo é exibido dentro de runOnUiThread.
      activity.runOnUiThread(
              new Runnable() {
                 public void run() {
                    FragmentManager fragmentManager =
                            ((AppCompatActivity) activity).getSupportFragmentManager();

                    // se a Activity já salvou seu estado (app sendo pausado
                    // ou fechado), exibir o diálogo causaria uma exceção
                    if (fragmentManager.isStateSaved())
                       return;

                    showSystemBars(); // sai do modo imersivo
                    dialogIsDisplayed = true;
                    gameResult.setCancelable(false); // fecha só pelo botão
                    gameResult.show(fragmentManager, "results");
                 }
              }
      );
   }

   // chamado pelo botão "Reset Game" do diálogo: começa um novo jogo
   void resetGameFromDialog() {
      dialogIsDisplayed = false;
      newGame();
   }

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

   // Verifica, a cada quadro do game loop, se a bala colidiu com algum
   // alvo ou com o bloqueador.
   // - alvo atingido: o alvo é removido e o jogador ganha HIT_REWARD segundos
   // - bloqueador atingido: a bala volta e o jogador perde MISS_PENALTY segundos
   public void testForCollisions() {
      // testa os alvos apenas se houver uma bala na tela
      if (cannon.getCannonball() != null &&
              cannon.getCannonball().isOnScreen()) {
         for (int n = 0; n < targets.size(); n++) {
            if (cannon.getCannonball().collidesWith(targets.get(n))) {
               targets.get(n).playSound(); // som de alvo atingido

               // soma a recompensa ao tempo restante
               timeLeft += targets.get(n).getHitReward();

               cannon.removeCannonball(); // remove a bala do jogo
               targets.remove(n); // remove o alvo atingido
               --n; // a lista diminuiu, então o índice volta uma posição
               break; // a bala já foi removida, não há mais o que testar
            }
         }
      }
      else {
         // a bala saiu da tela: remove para liberar um novo disparo
         cannon.removeCannonball();
      }

      // verifica se a bala (se ainda existir) colidiu com o bloqueador
      if (cannon.getCannonball() != null &&
              cannon.getCannonball().collidesWith(blocker)) {
         blocker.playSound(); // som de bloqueador atingido

         // inverte a velocidade horizontal: a bala volta em direção ao canhão
         cannon.getCannonball().reverseVelocityX();

         // desconta a penalidade do tempo restante
         timeLeft -= blocker.getMissPenalty();
      }
   }

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

   // chamado pelo Android quando o usuário toca a tela
   // (no emulador, o clique do mouse conta como toque)
   @Override
   public boolean onTouchEvent(MotionEvent e) {
      int action = e.getAction(); // tipo de evento de toque

      // ACTION_DOWN: o dedo encostou na tela
      // ACTION_MOVE: o dedo está sendo arrastado
      // nos dois casos o canhão mira e tenta disparar
      if (action == MotionEvent.ACTION_DOWN ||
              action == MotionEvent.ACTION_MOVE) {
         alignAndFireCannonball(e);
      }

      return true; // indica que o evento de toque foi tratado aqui
   }

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

                  // a cada quadro: move os elementos, testa as colisões
                  // e desenha o resultado na tela
                  updatePositions(elapsedTimeMS); // atualiza o estado do jogo
                  testForCollisions(); // testa colisões entre GameElements
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

   // DialogFragment que exibe o resultado do jogo (Fig. 6.27).
   // Precisa ser public static para que o Android consiga recriá-lo
   // sozinho, por exemplo depois que a Activity é recriada.
   public static class GameResultDialogFragment extends DialogFragment {
      // chaves usadas para guardar os dados da partida no Bundle
      private static final String ARG_MESSAGE_ID = "messageId";
      private static final String ARG_SHOTS_FIRED = "shotsFired";
      private static final String ARG_TOTAL_TIME = "totalElapsedTime";

      // Cria o diálogo com os dados da partida que terminou. Os dados vão
      // em um Bundle (setArguments) porque o Android salva e restaura esses
      // argumentos automaticamente quando recria o fragment.
      public static GameResultDialogFragment newInstance(int messageId,
                                                         int shotsFired, double totalElapsedTime) {
         Bundle arguments = new Bundle();
         arguments.putInt(ARG_MESSAGE_ID, messageId);
         arguments.putInt(ARG_SHOTS_FIRED, shotsFired);
         arguments.putDouble(ARG_TOTAL_TIME, totalElapsedTime);

         GameResultDialogFragment dialog = new GameResultDialogFragment();
         dialog.setArguments(arguments);
         return dialog;
      }

      // cria o AlertDialog e o retorna
      @Override
      public Dialog onCreateDialog(Bundle bundle) {
         // recupera os dados da partida guardados em newInstance
         Bundle arguments = requireArguments();
         int messageId = arguments.getInt(ARG_MESSAGE_ID);
         int shotsFired = arguments.getInt(ARG_SHOTS_FIRED);
         double totalElapsedTime = arguments.getDouble(ARG_TOTAL_TIME);

         // título: "You win!" ou "You lose!" (String de recurso messageId)
         AlertDialog.Builder builder =
                 new AlertDialog.Builder(getActivity());
         builder.setTitle(getResources().getString(messageId));

         // mensagem: número de disparos e tempo total da partida
         builder.setMessage(getResources().getString(
                 R.string.results_format, shotsFired, totalElapsedTime));
         builder.setPositiveButton(R.string.reset_game,
                 new DialogInterface.OnClickListener() {
                    // chamado quando o botão "Reset Game" é pressionado
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                       CannonView cannonView =
                               requireActivity().findViewById(R.id.cannonView);
                       if (cannonView != null)
                          cannonView.resetGameFromDialog(); // novo jogo
                    }
                 }
         );

         return builder.create(); // retorna o AlertDialog
      }
   }
}
 