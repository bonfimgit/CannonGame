// GameElement.java
// Representa um elemento do jogo delimitado por um retângulo (Fig. 6.7)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {
   protected CannonView view; // a view que contém este GameElement
   protected Paint paint = new Paint(); // Paint para desenhar este GameElement
   protected Rect shape; // os limites retangulares do GameElement
   private float velocityY; // a velocidade vertical deste GameElement
   private int soundId; // o som associado a este GameElement

   // construtor público
   public GameElement(CannonView view, int color, int soundId, int x,
      int y, int width, int length, float velocityY) {
      this.view = view;
      paint.setColor(color);
      shape = new Rect(x, y, x + width, y + length); // define os limites
      this.soundId = soundId;
      this.velocityY = velocityY;
   }

   // atualiza a posição do GameElement e verifica colisões com as bordas
   public void update(double interval) {
      // atualiza a posição vertical
      shape.offset(0, (int) (velocityY * interval));

      // se este GameElement colidir com a borda, inverte a direção
      if (shape.top < 0 && velocityY < 0 ||
         shape.bottom > view.getScreenHeight() && velocityY > 0)
         velocityY *= -1; // inverte a velocidade deste GameElement
   }

   // desenha este GameElement no Canvas informado
   public void draw(Canvas canvas) {
      canvas.drawRect(shape, paint);
   }

   // toca o som que corresponde a este tipo de GameElement
   public void playSound() {
      view.playSound(soundId);
   }
}
