// Target.java
// Subclasse de GameElement personalizada para o Target (Fig. 6.9)
package com.deitel.cannongame;

public class Target extends GameElement {
   private int hitReward; // a recompensa por acertar este alvo

   // construtor
   public Target(CannonView view, int color, int hitReward, int x, int y,
      int width, int length, float velocityY) {
      super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length,
         velocityY);
      this.hitReward = hitReward;
   }

   // retorna a recompensa deste Target
   public int getHitReward() {
      return hitReward;
   }
}
