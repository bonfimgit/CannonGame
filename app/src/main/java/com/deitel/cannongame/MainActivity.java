// MainActivity.java
// MainActivity exibe o MainActivityFragment (Fig. 6.5)
package com.deitel.cannongame;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
   // chamado quando o app é iniciado pela primeira vez
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
   }
}
