package tk.ansidev.torch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: ansidev
 * Date: 27 Aug, 2015
 * Version: 1.0
 */
public class MainActivity extends ActionBarActivity {

    private static final String TORCH_ON = "ĐANG BẬT";
    private static final String TORCH_OFF = "ĐANG TẮT";

    //Biến kiểm tra trạng thái đèn pin
//    boolean isTorchOn = true;
    boolean isTorchOn = false;

//    private CameraManager cameraManager;

    public TextView txtStatus;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mBuilder;
    private CameraDevice mCameraDevice;
    private String flashId;
    private CameraManager mCameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            init();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        Context context = this;
        txtStatus = (TextView) findViewById(R.id.txtStatus);
//        txtStatus.setText(TORCH_ON);
    }

    private void init() throws CameraAccessException {
        mCameraManager = (CameraManager) MainActivity.this.getSystemService(Context.CAMERA_SERVICE);
        for (String cameraId : mCameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                flashId = cameraId;
                break;
            }
        }
        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(flashId);
//            final String cameraId = cameraManager.getCameraIdList()[0];
        boolean hasFlash = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        if (!hasFlash) {
            //Thiết bị không có đèn flash
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Lỗi");
            alertDialog.setMessage("Xin lỗi, thiết bị của bạn không được hỗ trợ để chạy ứng dụng!");
            alertDialog.setButton(1, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alertDialog.show();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickBtnSwitch(View view) {
        changeFlashStatus();
    }

    private void changeFlashStatus() {
        try {
            if (!isTorchOn) {
                mCameraManager.openCamera(flashId, new MyCameraDeviceStateCallback(), null);
                isTorchOn = true;
//                txtStatus.setText(TORCH_ON);
            } else {
                mCameraDevice.close();
                isTorchOn = false;
//                txtStatus.setText(TORCH_OFF);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    class MyCameraDeviceStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //flash on, default is on
                mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
                mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                List<Surface> list = new ArrayList<Surface>();
                mSurfaceTexture = new SurfaceTexture(1);
                Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }

        private Size getSmallestSize(String cameraId) throws CameraAccessException {
//            cameraId = flashId;
            Size[] outputSizes = mCameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class);
            if (outputSizes == null || outputSizes.length == 0) {
                throw new IllegalStateException(
                        "Camera " + cameraId + "doesn't support any outputSize.");
            }
            Size chosen = outputSizes[0];
            for (Size s : outputSizes) {
                if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                    chosen = s;
                }
            }
            return chosen;
        }

        class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {

            @Override
            public void onConfigured(CameraCaptureSession session) {
                mSession = session;
                try {
                    mSession.setRepeatingRequest(mBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        }

    }
}
