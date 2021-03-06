package edu.oregonstate.rthowerton.encryption;

import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.FileNotFoundException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;

import java.lang.Math;
import java.lang.Thread;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

//import static com.firebase.ui.auth.AuthUI.getApplicationContext;


public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;

    // Variables for all threads to use
    protected TextView textTargetURI;
    protected ImageView targetImage;
    protected ImageView showImage;
    protected BitmapDrawable toChange;
    protected Bitmap bitmap;
    protected int BLOCKSIZE = 192;
    protected int ITERATIONS = 50;
    protected byte[] iv = new byte[16];

    protected int totalRndForSubstitution, totalRndForPermutation;
    protected byte[] substitutionKey, permutationKey;
    // Key
    private byte[] key = "Example128BitKey".getBytes();
    protected SecretKey secret = new SecretKeySpec(key, "AES");
    protected Bitmap temp1;
    protected Bitmap temp2;
    protected Bitmap temp3;
    protected IvParameterSpec s;

    protected Cipher cipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply onClickListeners to buttons
        Button buttonLoadImage = findViewById(R.id.loadImage);
        Button buttonEncryptImage = findViewById(R.id.encryptImage);
        Button buttonDecryptImage = findViewById(R.id.decryptImage);

        textTargetURI = findViewById(R.id.targetURI);
        targetImage = findViewById(R.id.targetImage);
        showImage = findViewById(R.id.showImage);
        // Load image from phone memory
        buttonLoadImage.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 0);
            }
        });

        // Encrypt button
        buttonEncryptImage.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                temp1 = toChange.getBitmap();
                encrypt(view, temp1);
            }
        });
        //Decrypt button
        buttonDecryptImage.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                //toChange = (BitmapDrawable) targetImage.getDrawable();
                //Bitmap change = toChange.getBitmap();
                //toChange = (BitmapDrawable) showImage.getDrawable();
                //Bitmap change = toChange.getBitmap();
                decrypt(view, temp2);
                if(temp1.getWidth() == temp3.getWidth()){
                    Toast myToaster = Toast.makeText(getApplicationContext(),"Same Width", Toast.LENGTH_LONG);
                    //myToaster.show();

                }
                else{
                    Toast myToaster = Toast.makeText(getApplicationContext(),"Unequal Widths", Toast.LENGTH_LONG);
                    //myToaster.show();
                }

                if(temp1.getHeight() == temp3.getHeight()){

                    Toast myToaster = Toast.makeText(getApplicationContext(),"Same Height", Toast.LENGTH_LONG);
                    //myToaster.show();

                }
                else{
                    Toast myToaster = Toast.makeText(getApplicationContext(),"Unequal Height", Toast.LENGTH_LONG);
                    //myToaster.show();
                }


                if(temp1.getConfig() == temp3.getConfig()){
                    Toast myToaster = Toast.makeText(getApplicationContext(),"Same Config", Toast.LENGTH_LONG);
                    //myToaster.show();

                }
                else{
                    Toast myToaster = Toast.makeText(getApplicationContext(),"Unequal Config", Toast.LENGTH_LONG);
                    //myToaster.show();
                }

                int count = 0;
                for(int i=0;i<temp1.getWidth();i++){
                    for(int j=0;j<temp1.getHeight();j++) {
                            if(temp1.getPixel(i,j) == temp3.getPixel(i,j)){

                            }
                            else{
                                count++;
                            }
                    }
                }
                Toast myToaster = Toast.makeText(getApplicationContext(),Integer.toString((temp1.getWidth()*temp1.getHeight() - count)), Toast.LENGTH_LONG);
                //myToaster.show();

                Log.d("santosh",Integer.toString(count));
                Log.d("checq","checq");
                if(temp1.sameAs(temp3)){
                    Log.d("works","works");

                }
                else{
                    Log.d("oops","oops");


                }
            }
        });
    }

    /*
     * Get the image to encrypt from phone memory, transform
     * image into bitmap, which is stored in the above class variables.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri targetUri = data.getData();
            textTargetURI.setText(targetUri.toString());
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                targetImage.setImageBitmap(bitmap);
                toChange = (BitmapDrawable) targetImage.getDrawable();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Encrypt the loaded image, which is stored in class variables.
     * This function creates several threads to operate on individual
     * blocks of the image.
     */

    private void encrypt(View view, Bitmap change) {
        // Temporary bitmap that will be placed in the app.
        temp2 = Bitmap.createBitmap(change.getWidth(), change.getHeight(), change.getConfig());
//        Log.i("imageWidth", temp.getWidth()+"");
//        Log.i("imageHeight", temp.getHeight()+"");

        // Number of blocks change is broken into horizontally and vertically
        int a = (int) Math.floor(change.getWidth() / BLOCKSIZE);
        //Log.i("widthSize", a+"");
        int b = (int) Math.floor(change.getHeight() / BLOCKSIZE);
        //Log.i("heightSize", b+"");

        // The necessary length of the AES key
        totalRndForPermutation = ITERATIONS * a * b * BLOCKSIZE * BLOCKSIZE;
        totalRndForSubstitution = ITERATIONS * a * b * ((BLOCKSIZE * BLOCKSIZE - (BLOCKSIZE * BLOCKSIZE) % 2) / 2) * 3;
        //Log.i("subSize", totalRndForSubstitution+"");

        // Byte arrays to encrypt. Does it work with empty arrays?
        substitutionKey = new byte[totalRndForSubstitution];
        permutationKey = new byte[totalRndForPermutation];


        // Start up a cipher
        try {
            //cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
            cipher = Cipher.getInstance("AES/CTR/NoPadding");

            byte[] iv = new byte[16];

            new SecureRandom().nextBytes(iv);
            s = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secret, s);
            //cipher.init(Cipher.ENCRYPT_MODE, secret);

            substitutionKey = cipher.doFinal(new byte[totalRndForSubstitution]);
//            Log.i("subLength", substitutionKey.length+"");
            permutationKey = cipher.doFinal(new byte[totalRndForPermutation]);
//            Log.i("permLength", permutationKey.length+"");
        } catch(GeneralSecurityException e){
            throw new IllegalStateException("Could not retrieve AES cipher", e);
        }

        // Array of Threads for Use
        Thread encryptThreads[][] = new Thread[a][b];

        // Index for the threads to access the block
        int index = 0;

        // Time the function
        long startTime = System.currentTimeMillis();
        // Fire off the threads
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
//                Thread encrypt = new Thread(new Encryptor(view, change, temp, i * BLOCKSIZE, j * BLOCKSIZE,
//                        0, Arrays.copyOfRange(substitutionKey, index, index + (BLOCKSIZE * BLOCKSIZE * 3 * ITERATIONS)),
//                        Arrays.copyOfRange(permutationKey, index, index + (BLOCKSIZE * BLOCKSIZE * ITERATIONS))));
//                encrypt.start();
                encryptThreads[i][j] = new Thread(new Encryptor(view, change, temp2, i * BLOCKSIZE, j * BLOCKSIZE,
                        0, Arrays.copyOfRange(substitutionKey, index, index + (BLOCKSIZE * BLOCKSIZE * 3 * ITERATIONS)),
                        Arrays.copyOfRange(permutationKey, index, index + (BLOCKSIZE * BLOCKSIZE * ITERATIONS))));
                encryptThreads[i][j].start();
                index += BLOCKSIZE * BLOCKSIZE;
            }
        }
        // Join all threads back
        for(int i = 0; i < a; i++){
            for(int j = 0; j < b; j++){
                try {
                    encryptThreads[i][j].join();
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        // Output the time as a toast
        Toast myToast = Toast.makeText(getApplicationContext(), duration+"", Toast.LENGTH_LONG);
        myToast.show();
        //targetImage.setImageBitmap(temp2);
        showImage.setImageBitmap(temp2);

    }

    private class Encryptor implements Runnable {
        // variables for this class only
        private View view;
        private int x;
        private int y;
        private Bitmap change, temp;
        private int ctr;
        // waaaaaaaaaaaaaaay too much memory usage
        byte[] substitutionKey, permutationKey;

        public Encryptor(View view, Bitmap bmp, Bitmap tmp, int x, int y, int index, byte[] subKey, byte[] permKey) {
            this.view = view;
            change = bmp;
            temp = tmp;
            this.x = x;
            this.y = y;
            ctr = index;
            this.substitutionKey = subKey;
            this.permutationKey = permKey;
        }

        public void run() {
            // Bitmap that contains the block of change
            Bitmap small = Bitmap.createBitmap(change, x, y, BLOCKSIZE, BLOCKSIZE);
            // Arrays to hold the pixels
            int[] pixels = new int[BLOCKSIZE * BLOCKSIZE];
            int[] afterSubstitution = new int[BLOCKSIZE * BLOCKSIZE];
            int[] permuter;
            int[] afterPermutation = new int[BLOCKSIZE * BLOCKSIZE];


            small.getPixels(pixels, 0, small.getWidth(), 0, 0, small.getWidth(), small.getHeight());
            for (int iterCount = 0; iterCount < ITERATIONS; iterCount++) {
                //Log.i("pixelLength", ""+pixels.length);
                // Substitution
                for (int i = 0; i < pixels.length; i += 2) {
                    int a1 = Color.alpha(pixels[i]);
                    int a2 = Color.alpha(pixels[i + 1]);
                    int r1 = Color.red(pixels[i]);
                    int r2 = Color.red(pixels[i + 1]);
                    int g1 = Color.green(pixels[i]);
                    int g2 = Color.green(pixels[i + 1]);
                    int b1 = Color.blue(pixels[i]);
                    int b2 = Color.blue(pixels[i + 1]);

                    int rt1 = getNewCouple(r1, r2, true);
                    int rt2 = r1 + r2 - rt1;

                    int gt1 = getNewCouple(g1, g2, true);
                    int gt2 = g1 + g2 - gt1;

                    int bt1 = getNewCouple(b1, b2, true);
                    int bt2 = b1 + b2 - bt1;

                    afterSubstitution[i] = Color.argb(a1, rt1, gt1, bt1);
                    afterSubstitution[i + 1] = Color.argb(a2, rt2, gt2, bt2);
                }
                this.ctr = 0;
                permuter = getNewPermutation();
                for (int j = 0; j < BLOCKSIZE * BLOCKSIZE; j++) {
                    afterPermutation[j] = afterSubstitution[permuter[j]];
                }
            }

            temp.setPixels(afterPermutation, 0, small.getWidth(), x, y, BLOCKSIZE, BLOCKSIZE);
        }

        /*
         * Translated from https://github.com/akshithg/TPEncryption/blob/master/plugin/popup.js
         *
         */
        private int getNewCouple(int comp1, int comp2, boolean enc) {
            ctr++;
            int rnd = this.substitutionKey[ctr - 1] & 0xFF;
            int sum = comp1 + comp2;
            if (sum <= 255) {
                if (enc)
                    rnd = (comp1 + rnd) % (sum + 1);
                else
                    rnd = (comp1 - rnd) % (sum + 1);
                if (rnd < 0) rnd = rnd + sum + 1;
                return rnd;
            } else {
                if (enc) {
                    rnd = 255 - (comp1 + rnd) % (511 - sum);
                    return rnd;
                } else {
                      rnd = (255 - comp1 - rnd) % (511 - sum);
                    while (rnd < (sum - 255)) {
                        rnd += 511 - sum;
                    }
                    return rnd;
                }
            }
        }

        /*
         * Translated from https://github.com/akshithg/TPEncryption/blob/master/plugin/popup.js
         *
         */
        private int[] getNewPermutation() {
            int[] permutation = new int[BLOCKSIZE * BLOCKSIZE];
            for (int z = 0; z < BLOCKSIZE * BLOCKSIZE; z++) {
                ctr++;
                permutation[z] = this.permutationKey[ctr - 1] & 0xFF;
            }
            int[] indices = new int[BLOCKSIZE * BLOCKSIZE];
            for (int i = 0; i < BLOCKSIZE * BLOCKSIZE; ++i)
            indices[i] = i;
            // Sort
            for (int j = 0; j < BLOCKSIZE; j++) {
                int key = permutation[j];
                int mirror = indices[j];
                int n = j - 1;
                while (n >= 0 && permutation[n] > key) {
                    permutation[n + 1] = permutation[n];
                    indices[n + 1] = indices[n];
                    n--;
                }
                permutation[n + 1] = key;
                indices[n + 1] = mirror;
            }
            return indices;
        }
    }
    private void decrypt(View view, Bitmap change) {
        // Temporary bitmap that will be placed in the app.
        temp3 = Bitmap.createBitmap(change.getWidth(), change.getHeight(), change.getConfig());
//        Log.i("imageWidth", temp.getWidth()+"");
//        Log.i("imageHeight", temp.getHeight()+"");

        // Number of blocks change is broken into horizontally and vertically
        int a = (int) Math.floor(change.getWidth() / BLOCKSIZE);
        //Log.i("widthSize", a+"");
        int b = (int) Math.floor(change.getHeight() / BLOCKSIZE);
        //Log.i("heightSize", b+"");

        // The necessary length of the AES key
        totalRndForPermutation = ITERATIONS * a * b * BLOCKSIZE * BLOCKSIZE;
        totalRndForSubstitution = ITERATIONS * a * b * ((BLOCKSIZE * BLOCKSIZE - (BLOCKSIZE * BLOCKSIZE) % 2) / 2) * 3;
        Log.i("subSize", totalRndForSubstitution+"");

        // Byte arrays to encrypt. Does it work with empty arrays?
        substitutionKey = new byte[totalRndForSubstitution];
        permutationKey = new byte[totalRndForPermutation];

        // Start up a cipher
        try {
            //cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
            //byte[] iv = new byte[16];
            //new SecureRandom().nextBytes(iv);
            //s = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secret, s);
            //cipher.init(Cipher.DECRYPT_MODE, secret);


            substitutionKey = cipher.doFinal(new byte[totalRndForSubstitution]);
//            Log.i("subLength", substitutionKey.length+"");

            permutationKey = cipher.doFinal(new byte[totalRndForPermutation]);
//            Log.i("permLength", permutationKey.length+"");
        } catch(GeneralSecurityException e){
            throw new IllegalStateException("Could not retrieve AES cipher", e);
        }

        // Array of Threads for Use
        Thread decryptThreads[][] = new Thread[a][b];

        // Index for the threads to access the block
        int index = 0;

        // Time the function
        long startTime = System.currentTimeMillis();
        // Fire off the threads
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
//                Thread encrypt = new Thread(new Encryptor(view, change, temp, i * BLOCKSIZE, j * BLOCKSIZE,
//                        0, Arrays.copyOfRange(substitutionKey, index, index + (BLOCKSIZE * BLOCKSIZE * 3 * ITERATIONS)),
//                        Arrays.copyOfRange(permutationKey, index, index + (BLOCKSIZE * BLOCKSIZE * ITERATIONS))));
//                encrypt.start();

                decryptThreads[i][j] = new Thread(new Decryptor(view, change, temp3, i * BLOCKSIZE, j * BLOCKSIZE,
                        0, Arrays.copyOfRange(substitutionKey, index, index + (BLOCKSIZE * BLOCKSIZE * 3 * ITERATIONS)),
                        Arrays.copyOfRange(permutationKey, index, index + (BLOCKSIZE * BLOCKSIZE * ITERATIONS))));
                decryptThreads[i][j].start();
                index += BLOCKSIZE * BLOCKSIZE;
            }
        }
        // Join all threads back
        for(int i = 0; i < a; i++){
            for(int j = 0; j < b; j++){
                try {
                    decryptThreads[i][j].join();
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        // Output the time as a toast
        Toast myToast = Toast.makeText(getApplicationContext(), duration+"", Toast.LENGTH_LONG);
        myToast.show();
        //targetImage.setImageBitmap(temp);
        showImage.setImageBitmap(temp3);

    }

    private class Decryptor implements Runnable {
        // variables for this class only
        private View view;
        private int x;
        private int y;
        private Bitmap change, temp;
        private int ctr;
        // waaaaaaaaaaaaaaay too much memory usage
        byte[] substitutionKey, permutationKey;

        public Decryptor(View view, Bitmap bmp, Bitmap tmp, int x, int y, int index, byte[] subKey, byte[] permKey) {
            this.view = view;
            change = bmp;
            temp = tmp;
            this.x = x;
            this.y = y;
            ctr = index;
            this.substitutionKey = subKey;
            this.permutationKey = permKey;
        }

        public void run() {
            // Bitmap that contains the block of change
            Bitmap small = Bitmap.createBitmap(change, x, y, BLOCKSIZE, BLOCKSIZE);
            // Arrays to hold the pixels
            int[] pixels = new int[BLOCKSIZE * BLOCKSIZE];
            int[] afterSubstitution = new int[BLOCKSIZE * BLOCKSIZE];
            int[] permuter;
            int[] afterPermutation = new int[BLOCKSIZE * BLOCKSIZE];


            small.getPixels(pixels, 0, small.getWidth(), 0, 0, small.getWidth(), small.getHeight());
            for (int iterCount = 0; iterCount < ITERATIONS; iterCount++) {
                //Log.i("pixelLength", ""+pixels.length);
                // Substitution
                this.ctr = 0;
                permuter = getNewPermutation();
                for (int j = 0; j < BLOCKSIZE * BLOCKSIZE; j++) {
                    afterPermutation[j] = afterSubstitution[permuter[j]];
                }

                for (int i = 0; i < pixels.length; i += 2) {
                    int a1 = Color.alpha(pixels[i]);
                    int a2 = Color.alpha(pixels[i + 1]);
                    int r1 = Color.red(pixels[i]);
                    int r2 = Color.red(pixels[i + 1]);
                    int g1 = Color.green(pixels[i]);
                    int g2 = Color.green(pixels[i + 1]);
                    int b1 = Color.blue(pixels[i]);
                    int b2 = Color.blue(pixels[i + 1]);

                    int rt1 = getNewCouple(r1, r2, false);
                    int rt2 = r1 + r2 - rt1;

                    int gt1 = getNewCouple(g1, g2, false);
                    int gt2 = g1 + g2 - gt1;

                    int bt1 = getNewCouple(b1, b2, false);
                    int bt2 = b1 + b2 - bt1;

                    afterSubstitution[i] = Color.argb(a1, rt1, gt1, bt1);
                    afterSubstitution[i + 1] = Color.argb(a2, rt2, gt2, bt2);
                }

            }

            temp.setPixels(afterPermutation, 0, small.getWidth(), x, y, BLOCKSIZE, BLOCKSIZE);
        }

        /*
         * Translated from https://github.com/akshithg/TPEncryption/blob/master/plugin/popup.js
         *
         */
        private int getNewCouple(int comp1, int comp2, boolean enc) {
            ctr++;
            int rnd = this.substitutionKey[ctr - 1] & 0xFF;
            int sum = comp1 + comp2;
            if (sum <= 255) {
                if (enc)
                    rnd = (comp1 + rnd) % (sum + 1);
                else
                    rnd = (comp1 - rnd) % (sum + 1);
                if (rnd < 0) rnd = rnd + sum + 1;
                return rnd;
            } else {
                if (enc) {
                    rnd = 255 - (comp1 + rnd) % (511 - sum);
                    return rnd;
                } else {
                    rnd = (255 - comp1 - rnd) % (511 - sum);
                    while (rnd < (sum - 255)) {
                        rnd += 511 - sum;
                    }
                    return rnd;
                }
            }
        }

        /*
         * Translated from https://github.com/akshithg/TPEncryption/blob/master/plugin/popup.js
         *
         */
        private int[] getNewPermutation() {
            int[] permutation = new int[BLOCKSIZE * BLOCKSIZE];
            for (int z = 0; z < BLOCKSIZE * BLOCKSIZE; z++) {
                ctr++;
                permutation[z] = this.permutationKey[ctr - 1] & 0xFF;
            }
            int[] indices = new int[BLOCKSIZE * BLOCKSIZE];
            for (int i = 0; i < BLOCKSIZE * BLOCKSIZE; ++i)
                indices[i] = i;
            // Sort
            for (int j = 0; j < BLOCKSIZE; j++) {
                int key = permutation[j];
                int mirror = indices[j];
                int n = j - 1;
                while (n >= 0 && permutation[n] > key) {
                    permutation[n + 1] = permutation[n];
                    indices[n + 1] = indices[n];
                    n--;
                }
                permutation[n + 1] = key;
                indices[n + 1] = mirror;
            }
            return indices;
        }
    }
}