package com.example.toma_foto;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;


import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private final String CARPETA_RAIZ="misImagenesPrueba/";
    private final String RUTA_IMAGEN=CARPETA_RAIZ+"misFotos";
    String fotoc, path;
    EditText codemple;
    TextView nomemple;
    Button opciones;
    ImageView foto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //se invocan los recursos y se trasladan los datos a cada una de las varibles creadas en el main

        codemple = findViewById(R.id.codemple);
        nomemple = findViewById(R.id.nomemple);
        foto = findViewById(R.id.imgfoto);
        opciones = findViewById(R.id.btnseleccion);

        if (validapermiso())
        {
            opciones.setEnabled(true);
        }else{
            opciones.setEnabled(false);
        }




        opciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Opciones();
            }
        });





    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==100){
            if (grantResults.length==2 && grantResults[0]==PackageManager.PERMISSION_GRANTED
                    && grantResults[1]==PackageManager.PERMISSION_GRANTED){
                opciones.setEnabled(true);
            }
            else{
                solicitarpermisosManual();
            }
        }
    }

    private void solicitarpermisosManual() {

        final CharSequence[] opciones= {"Si", "No"};
        final AlertDialog.Builder alert=new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Configuracion de permisos de forma manual?????");
        alert.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (opciones[i].equals("si")){
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("Package",getPackageName(),null);
                    intent.setData(uri);
                    startActivity(intent);


                }else{
                    Toast.makeText(getApplicationContext(),"los permisos no fueron aceptados",Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

            }

        });

        alert.show();

    }

    private boolean validapermiso() {
        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            return true;
        }


        if ((ContextCompat.checkSelfPermission(this, CAMERA)== PackageManager.PERMISSION_GRANTED)&&
                (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)){
            return true;
        }
        if ((shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))||(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))){
            cargarrecomendacion();
        }
        else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA},100);
        }


        return false;
    }

    private void cargarrecomendacion() {
        AlertDialog.Builder dialogo=new AlertDialog.Builder(MainActivity.this);
        dialogo.setTitle("permisos desactivados");
        dialogo.setMessage("debe aceptar los permisos correspondientes ");
        dialogo.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA},100);
            }
        });

        dialogo.show();
    }

    private void Opciones() {
        final CharSequence[] opciones= {"Tomar Foto", "Buscar Codigo", "Guardar","Cancelar"};
        final AlertDialog.Builder alert=new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("selecciones una opcion");
        alert.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (opciones[i].equals("Tomar Foto")){
                   tomarfotografia();

                }else{
                    if (opciones[i].equals("cargar imagen")){
                        //logica para cargar una imagen de nuestra galeria en un imageview
                        Intent intent=new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent,"seleccionar una foto"),10);
                    }
                    else{
                        if (opciones[i].equals("Guardar")){
                            //logica para cargar una imagen de nuestra galeria en un imageview
                            Toast.makeText(getApplicationContext(),"Foto Guardada con Exito...!!!",Toast.LENGTH_SHORT).show();
                            limpiarcampos();
                        }
                        else{
                            if (opciones[i].equals("Buscar Codigo")){
                                //logica para cargar una imagen de nuestra galeria en un imageview
                                Toast.makeText(getApplicationContext(),"Buscando codigo...!!!",Toast.LENGTH_SHORT).show();
                                Buscarcodigo(codemple.getText().toString());
                            }else
                            {

                                    dialog.dismiss();
                                }
                            }
                        }


                    }


            }

        });

        alert.show();
    }

    private void Buscarcodigo(final String dato) {
        //busqueda de informacion en el web service
        /*TareaWSConsulta tarea = new TareaWSConsulta();
        tarea.execute();*/


        //esto acciona el mecanismo de subida de informacion en este caso la foto que necesitamos enviar
        UploaderFoto nuevaTarea = new UploaderFoto();
        nuevaTarea.execute(foto);




    }


    class UploaderFoto extends AsyncTask {

        ProgressDialog pDialog;
        String miFoto = "";


        @Override
        protected Object doInBackground(Object[] objects) {
            miFoto = (String) objects[0];
            try {
                HttpClient httpclient = new DefaultHttpClient();
                httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                HttpPost httppost = new HttpPost("http://servidor.com/up.php");
                File file = new File(miFoto);
                MultipartEntity mpEntity = new MultipartEntity();
                ContentBody foto = new FileBody(file, «image/jpeg»);
                mpEntity.addPart(«fotoUp», foto);
                httppost.setEntity(mpEntity);
                httpclient.execute(httppost);
                httpclient.getConnectionManager().shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Subiendo la imagen, espere.");
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setCancelable(true);
            pDialog.show();
        }
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            pDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }













    private class TareaWSConsulta extends AsyncTask<String, Integer, Boolean> {
        String res;

        protected Boolean doInBackground(String... params) {
            boolean resul = true;

            final String NAMESPACE = "http://system.gfacepbs.com.gt/Servicio/";
            final String URL = "http://system.gfacepbs.com.gt/Servicio/WebService.asmx";
            final String METHOD_NAME = "empleado";
            final String SOAP_ACTION = "http://system.gfacepbs.com.gt/Servicio/empleado";

            SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
            request.addProperty("codigo", Integer.parseInt(codemple.getText().toString()));

            SoapSerializationEnvelope envelope =
                    new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;

            envelope.setOutputSoapObject(request);

            HttpTransportSE transporte = new HttpTransportSE(URL);

            try {
                transporte.call(SOAP_ACTION, envelope);

                SoapPrimitive resultado_xml = (SoapPrimitive) envelope.getResponse();
                res = resultado_xml.toString();

                transporte.getServiceConnection().disconnect();

            } catch (Exception e) {
                res = "Revise su conexión a Internet";
                resul = false;
            }

            return resul;
        }

        protected void onPostExecute(Boolean result) {

            if (result) {
                //Rellenamos la lista con los nombres de los clientes
              //  Toast.makeText(getApplicationContext(), "Si lo hice " + res, Toast.LENGTH_LONG ).show();

            } else {
                Toast.makeText(getApplicationContext(), "No lo hice ", Toast.LENGTH_LONG ).show();

            }

            nomemple.setText(res);

        }

    }

    private void tomarfotografia() {
        String nombreimagen="";
        File fileimagen=new File(Environment.getExternalStorageDirectory(),RUTA_IMAGEN);
        boolean iscreada = fileimagen.exists();

        if (iscreada==false){
            iscreada=fileimagen.mkdirs();
        }
        if (iscreada==true){
            nombreimagen=codemple.getText().toString()+".jpg";
        }

        path=Environment.getExternalStorageDirectory()+File.separator+RUTA_IMAGEN+File.separator+nombreimagen;
        File imagen = new File(path);

        Intent takePictureIntent=null;
        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            String autoridad = getApplicationContext().getPackageName()+".provider";
            Uri imagenUri = FileProvider.getUriForFile(this,autoridad,imagen);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imagenUri);
        }
        else{
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(imagen));

        }

        startActivityForResult(takePictureIntent, 20);




       /* Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            startActivityForResult(takePictureIntent, 20);
        }*/
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            MediaScannerConnection.scanFile(this, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ruta de almacenamiento","Path"+path);
                }
            });

            Bitmap bitmap=BitmapFactory.decodeFile(path);
            foto.setImageBitmap(bitmap);




        /*    Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            foto.setImageBitmap(imageBitmap);*/
        }


    }


    private void limpiarcampos() {

        codemple.setText("");
        nomemple.setText("");
        foto.setImageResource(R.drawable.icono);



    }


}


