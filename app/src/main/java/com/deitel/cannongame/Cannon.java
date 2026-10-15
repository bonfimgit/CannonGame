// Cannon.java
// Representa o canhão e dispara a bala de canhão (Figs. 6.10 a 6.14)
package com.deitel.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
   private int baseRadius; // raio da base do canhão
   private int barrelLength; // comprimento do cano do canhão
   private Point barrelEnd = new Point(); // extremidade do cano do canhão
   private double barrelAngle; // ângulo do cano do canhão
   private Cannonball cannonball; // a bala de canhão
   private Paint paint = new Paint(); // Paint usado para desenhar o canhão
   private CannonView view; // view que contém o canhão

   // construtor
   public Cannon(CannonView view, int baseRadius, int barrelLength,
      int barrelWidth) {
      this.view = view;
      this.baseRadius = baseRadius;
      this.barrelLength = barrelLength;
      paint.setStrokeWidth(barrelWidth); // define a largura do cano
      paint.setColor(Color.BLACK); // a cor do canhão é preta
      align(Math.PI / 2); // cano do canhão apontando para a direita
   }

   // alinha o cano do canhão ao ângulo informado
   public void align(double barrelAngle) {
      this.barrelAngle = barrelAngle;
      barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));
      barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle)) +
         view.getScreenHeight() / 2;
   }

   // cria e dispara a bala na direção para a qual o canhão aponta
   public void fireCannonball() {
      // calcula o componente x da velocidade da bala
      int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
         view.getScreenWidth() * Math.sin(barrelAngle));

      // calcula o componente y da velocidade da bala
      int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
         view.getScreenWidth() * -Math.cos(barrelAngle));

      // calcula o raio da bala
      int radius = (int) (view.getScreenHeight() *
         CannonView.CANNONBALL_RADIUS_PERCENT);

      // constrói a bala e a posiciona dentro do canhão
      cannonball = new Cannonball(view, Color.BLACK,
         CannonView.CANNON_SOUND_ID, -radius,
         view.getScreenHeight() / 2 - radius, radius, velocityX,
         velocityY);

      cannonball.playSound(); // toca o som do disparo
   }

   // desenha o canhão no Canvas
   public void draw(Canvas canvas) {
      // desenha o cano do canhão
      canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x,
         barrelEnd.y, paint);

      // desenha a base do canhão
      canvas.drawCircle(0, (int) view.getScreenHeight() / 2,
         (int) baseRadius, paint);
   }

   // retorna a bala que este canhão disparou
   public Cannonball getCannonball() {
      return cannonball;
   }

   // remove a bala do jogo
   public void removeCannonball() {
      cannonball = null;
   }
}
