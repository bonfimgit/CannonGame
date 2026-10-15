// MainActivityFragment.java
// MainActivityFragment cria e gerencia um CannonView (Fig. 6.6)
package com.deitel.cannongame;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class MainActivityFragment extends Fragment {
   private CannonView cannonView; // view personalizada que exibe o jogo

   // chamado quando a View do Fragment precisa ser criada
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);

      // infla o layout fragment_main.xml
      View view =
         inflater.inflate(R.layout.fragment_main, container, false);

      // obtém uma referência ao CannonView
      cannonView = (CannonView) view.findViewById(R.id.cannonView);
      return view;
   }

   // configura o controle de volume depois que a Activity é criada
   @SuppressWarnings("deprecation") // mantido conforme o livro (Seção 6.7)
   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);

      // permite que os botões de volume ajustem o volume do jogo
      getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
   }

   // quando a MainActivity é pausada, encerra o jogo
   @Override
   public void onPause() {
      super.onPause();
      cannonView.stopGame(); // encerra o jogo
   }

   // quando a MainActivity é destruída, MainActivityFragment libera recursos
   @Override
   public void onDestroy() {
      super.onDestroy();
      cannonView.releaseResources();
   }
}
