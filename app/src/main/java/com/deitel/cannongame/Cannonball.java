// Cannonball.java
// Representa a bala que o canhão dispara (Figs. 6.15 a 6.18)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Rect;

public class Cannonball extends GameElement {
   private float velocityX;
   private boolean onScreen;

   // construtor
   public Cannonball(CannonView view, int color, int soundId, int x,
      int y, int radius, float velocityX, float velocityY) {
      super(view, color, soundId, x, y,
         2 * radius, 2 * radius, velocityY);
      this.velocityX = velocityX;
      onScreen = true;
   }

   // obtém o raio da bala
   private int getRadius() {
      return (shape.right - shape.left) / 2;
   }

   // testa se a bala colide com o GameElement informado
   public boolean collidesWith(GameElement element) {
      return (Rect.intersects(shape, element.shape) && velocityX > 0);
   }

   // retorna true se a bala estiver na tela
   public boolean isOnScreen() {
      return onScreen;
   }

   // inverte a velocidade horizontal da bala
   public void reverseVelocityX() {
      velocityX *= -1;
   }

   // atualiza a posição da bala
   @Override
   public void update(double interval) {
      super.update(interval); // atualiza a posição vertical da bala

      // atualiza a posição horizontal
      shape.offset((int) (velocityX * interval), 0);

      // se a bala sair da tela
      if (shape.top < 0 || shape.left < 0 ||
         shape.bottom > view.getScreenHeight() ||
         shape.right > view.getScreenWidth())
         onScreen = false; // marca para ser removida
   }

   // desenha a bala no Canvas informado
   @Override
   public void draw(Canvas canvas) {
      canvas.drawCircle(shape.left + getRadius(),
         shape.top + getRadius(), getRadius(), paint);
   }
}
