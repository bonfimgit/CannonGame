// Blocker.java
// Subclasse de GameElement personalizada para o Blocker (Fig. 6.8)
package com.deitel.cannongame;

public class Blocker extends GameElement {
   private int missPenalty; // a penalidade por acertar este Blocker

   // construtor
   public Blocker(CannonView view, int color, int missPenalty, int x,
      int y, int width, int length, float velocityY) {
      super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length,
         velocityY);
      this.missPenalty = missPenalty;
   }

   // retorna a penalidade deste Blocker
   public int getMissPenalty() {
      return missPenalty;
   }
}
