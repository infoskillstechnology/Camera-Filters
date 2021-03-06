package com.davidcryer.camerafilters.screens.filter;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.davidc.uiwrapper.UiWrapper;
import com.davidcryer.camerafilters.R;
import com.davidcryer.camerafilters.framework.opencv.ColorEffect;
import com.davidcryer.camerafilters.framework.opencv.ImageEffect;
import com.davidcryer.camerafilters.framework.opencv.ImageManipulator;
import com.davidcryer.camerafilters.framework.opencv.OpenCvInitialiser;
import com.davidcryer.camerafilters.framework.uiwrapper.UiModelFactory;
import com.davidcryer.camerafilters.helpers.PermissionHelper;
import com.davidcryer.camerafilters.helpers.PictureHelper;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class FilterUiWrapper extends UiWrapper<FilterUi, FilterUi.Listener, FilterUiModel> {
    private final static int PERMISSION_REQUEST_CAMERA = 1;
    private final static int PERMISSION_REQUEST_EXT_WRITE = 2;

    private FilterUiWrapper(@NonNull FilterUiModel uiModel) {
        super(uiModel);
    }

    public static FilterUiWrapper newInstance() {
        return new FilterUiWrapper(UiModelFactory.createFilterUiModel());
    }

    public static @Nullable FilterUiWrapper savedState(final Bundle savedInstanceState) {
        final FilterUiModel model = savedUiModel(savedInstanceState);
        return model == null ? null : new FilterUiWrapper(model);
    }

    @Override
    protected FilterUi.Listener uiListener() {
        return new FilterUi.Listener() {
            @Override
            public void onStart(FilterUi ui) {
                initOpenCvLibrary(ui);
                getPermissions(ui);
            }

            @Override
            public void onStop(FilterUi ui) {
                ui.disableSurface();
            }

            @Override
            public void onDestroy(FilterUi ui) {
                ui.disableSurface();
            }

            @Override
            public void onPermissionsReturned(FilterUi ui, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                processReturnedPermissions(ui, requestCode, grantResults);
            }

            @Override
            public void onCameraViewStarted(int width, int height, FilterUi ui) {
                uiModel().imageManipulator(new ImageManipulator(height, width));
            }

            @Override
            public void onCameraViewStopped(FilterUi ui) {
                uiModel().imageManipulator(null);
            }

            @Override
            public boolean onBackPressed(FilterUi ui) {
                if (uiModel().isMenuOpen()) {
                    uiModel().closeMenu(ui);
                    return true;
                }
                return false;
            }

            @Override
            public void onClickMenuToggle(FilterUi ui) {
                toggleMenu(ui);
            }

            @Override
            public void onClickOnOffToggle(FilterUi ui) {
                uiModel().toggleRunningState(ui);
            }

            @Override
            public void onClickTakePhotograph(FilterUi ui) {
                if (hasExtWritePermission(ui)) {
                    ui.takePhotograph();
                } else {
                    requestExtWritePermission(ui);
                }
            }

            @Override
            public void onPictureTaken(FilterUi ui, byte[] data, int height, int width) {
                final String fileName = PictureHelper.defaultName();
                final ImageManipulator imageManipulator = new ImageManipulator(height, width);
                imageManipulator.imageProcessing(uiModel().imageEffect());
                imageManipulator.colorProcessing(uiModel().colorEffect());
                PictureHelper.saveAndRecycle(imageManipulator.process(data, height, width), fileName);
                ui.showToast(String.format(ui.activity().getString(R.string.filter_photo_saved), fileName));
                imageManipulator.release();
            }

            @Override
            public void onClickMenuColorProcessing(FilterUi ui, String effect) {
                final ColorEffect colorEffect = ColorEffect.effect(effect);
                if (colorEffect != null) {
                    uiModel().colorEffect(colorEffect);
                }
            }

            @Override
            public void onClickMenuImageProcessing(FilterUi ui, String effect) {
                final ImageEffect imageEffect = ImageEffect.effect(effect);
                if (imageEffect != null) {
                    uiModel().imageEffect(imageEffect);
                }
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame, FilterUi ui) {
                return uiModel().imageManipulator().onCameraFrame(inputFrame);
            }
        };
    }

    private void getPermissions(final FilterUi ui) {
        if (shouldRequestCameraPermission(ui)) {
            requestCameraPermission(ui);
        } else if (shouldRequestExtWritePermission(ui)) {
            requestExtWritePermission(ui);
        }
    }

    private boolean shouldRequestCameraPermission(final FilterUi ui) {
        return !hasCameraPermission(ui) && !uiModel().hasAskedForCameraPermission();
    }

    private boolean hasCameraPermission(final FilterUi ui) {
        return PermissionHelper.has(Manifest.permission.CAMERA, ui.activity());
    }

    private boolean shouldRequestExtWritePermission(final FilterUi ui) {
        return !hasExtWritePermission(ui) && !uiModel().hasAskedForExtWritePermission();
    }

    private boolean hasExtWritePermission(final FilterUi ui) {
        return PermissionHelper.has(Manifest.permission.WRITE_EXTERNAL_STORAGE, ui.activity());
    }

    private void requestExtWritePermission(final FilterUi ui) {
        uiModel().markAskingForExtWritePermission();
        PermissionHelper.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_EXT_WRITE, ui.fragment());
    }

    private void getExtWritePermissionIfAppropriate(final FilterUi ui) {
        if (shouldRequestExtWritePermission(ui)) {
            requestExtWritePermission(ui);
        }
    }

    private void initOpenCvLibrary(final FilterUi ui) {
        OpenCvInitialiser.init(ui.activity(), initCallback);
    }

    private final OpenCvInitialiser.Callback initCallback = new OpenCvInitialiser.Callback() {
        @Override
        public void onInitialised() {
            //TODO ideally block user interaction until opencv initialised, but this is a fast process
        }
    };

    private void requestCameraPermission(FilterUi ui) {
        uiModel().markAskingForCameraPermission();
        PermissionHelper.request(Manifest.permission.CAMERA, PERMISSION_REQUEST_CAMERA, ui.fragment());
    }

    private void processReturnedPermissions(final FilterUi ui, final int requestCode, final @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                if (PermissionHelper.isGranted(grantResults[0])) {
                    initOpenCvLibrary(ui);
                }
                getExtWritePermissionIfAppropriate(ui);
            } break;
        }
    }

    private void toggleMenu(FilterUi ui) {
        if (uiModel().isMenuOpen()) {
            uiModel().closeMenu(ui);
        } else {
            uiModel().openMenu(ui);
        }
    }
}
