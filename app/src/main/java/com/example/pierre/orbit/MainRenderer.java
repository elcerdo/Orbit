package com.example.pierre.orbit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer extends GestureDetector.SimpleOnGestureListener implements Renderer {
    private FloatBuffer verticesBufferSquare;
    private FloatBuffer verticesBufferArrow;
    private FloatBuffer verticesBufferCircle;

    private float verticesArraySquare[] = {
      -1,-1,0,1,
      1,-1,0,1,
      1,1,0,1,
      -1,1,0,1,
    };

    private float verticesArrayArrow[] = {
      1,0,0,1,
      .9f,.1f,0,1,
      .9f,.03f,0,1,
      0,.03f,0,1,
      0,-.03f,0,1,
      .9f,-.03f,0,1,
      .9f,-.1f,0,1,
    };

    public int counter;
    private long startTime;

    private int main_program;
    private int orbit_program;
    private int button_program;
    private int textures[];
    private Context context;

    public int width;
    public int height;
    public float button_size;
    private float sx;
    private float sy;

    private float eccentricity;
    private float latus_rectum;
    private float initial_phase;

    public boolean turn_left;
    public boolean turn_right;
    public boolean burn;

    public static FloatBuffer arrayToBuffer(float array[]) {
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(array.length * 4);
        byte_buffer.order(ByteOrder.nativeOrder());
        FloatBuffer float_buffer = byte_buffer.asFloatBuffer();
        float_buffer.put(array);
        float_buffer.position(0);
        return float_buffer;
    }

    public static FloatBuffer circleBuffer(int npts) {
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(4*4*(npts+1));
        byte_buffer.order(ByteOrder.nativeOrder());
        FloatBuffer float_buffer = byte_buffer.asFloatBuffer();
        float_buffer.put(0);
        float_buffer.put(0);
        float_buffer.put(0);
        float_buffer.put(1);
        for (int kk=0; kk<npts; kk++) {
            float theta = (float)(Math.PI*2*kk)/(npts-1);
            float_buffer.put((float)Math.cos(theta));
            float_buffer.put((float)Math.sin(theta));
            float_buffer.put(theta);
            float_buffer.put(1);
        }
        float_buffer.position(0);
        return float_buffer;
    }

    public static int loadShader(int type, String code) {
        int shaderId = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shaderId, code);
        GLES20.glCompileShader(shaderId);
        int isCompiled[] = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, isCompiled, 0);
        if(isCompiled[0] == GLES20.GL_FALSE) {
            String error_string = "SHADER COMPILE ERROR " + GLES20.glGetShaderInfoLog(shaderId);
            Log.e("Orbit", error_string);
            throw new RuntimeException(error_string);
        }
        return shaderId;
    }

    public static int loadTexture(InputStream is, int texture_id) {
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        return texture_id;
    }

    public static void assertStatus() {
        int error = GLES20.glGetError();
        if (error == GLES20.GL_NO_ERROR) return;
        String error_string = "OPENGL STATUS ERROR " + GLU.gluErrorString(error);
        Log.e("Orbit", error_string);
        throw new RuntimeException(error_string);
    }

    MainRenderer(Context context_) {
        Log.i("Orbit", "create renderer");
        counter = 2;
        width = height = -1;
        sx = .5f;
        sy = 0;
        verticesBufferSquare = arrayToBuffer(verticesArraySquare);
        verticesBufferArrow = arrayToBuffer(verticesArrayArrow);
        verticesBufferCircle = circleBuffer(1024);
        startTime = System.currentTimeMillis();
        context = context_;
        eccentricity = 0f;
        initial_phase = 0f;
        latus_rectum = .5f;
        turn_left = false;
        turn_right = false;
        burn = false;
        button_size = (float)context.getResources().getInteger(R.integer.button_size);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent evt) {
        counter ++;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent evt0, MotionEvent evt1, float dx, float dy) {
        assert( width > 0 && height > 0 );
        sx -= 2*dx/(float)Math.min(height, width);
        sy += 2*dy/(float)Math.min(height, width);
        return true;
    }

    @Override
    public void onDrawFrame(GL10 foo) {
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glLineWidth(10f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        float current_time = (System.currentTimeMillis() - startTime) / 1000f;

        {
            GLES20.glUseProgram(main_program);

            int position_attrib = GLES20.glGetAttribLocation(main_program, "aPosition");
            int color_uniform = GLES20.glGetUniformLocation(main_program, "uColor");
            int model_view_uniform = GLES20.glGetUniformLocation(main_program, "uModelView");
            int time_uniform = GLES20.glGetUniformLocation(main_program, "uTime");
            int mode_uniform = GLES20.glGetUniformLocation(main_program, "uMode");
            int tap_uniform = GLES20.glGetUniformLocation(main_program, "uTap");

            GLES20.glUniform1f(time_uniform, current_time);
            GLES20.glUniform2f(tap_uniform, sx, sy);

            float model_view_matrix[] = new float[16];
            Matrix.setIdentityM(model_view_matrix, 0);
            Matrix.scaleM(model_view_matrix, 0, 2f, 2f, 1f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // background
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                GLES20.glUniform4f(color_uniform, 0f, 0f, 0f, 1f);
                GLES20.glUniform1i(mode_uniform, 1);

                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferSquare);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferSquare.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }

            Matrix.setIdentityM(model_view_matrix, 0);
            Matrix.translateM(model_view_matrix, 0, 0f, 0f, 2f);
            Matrix.scaleM(model_view_matrix, 0, .2f, .2f, 1f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // circle
                GLES20.glUniform4f(color_uniform, counter % 3 == 0 ? 1.f : 0.f, counter % 3 == 1 ? 1.f : 0.f, counter % 3 == 2 ? 1.f : 0.f, 1.f);
                GLES20.glUniform1i(mode_uniform, 0);

                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferCircle);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferCircle.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }

            Matrix.setIdentityM(model_view_matrix, 0);
            Matrix.translateM(model_view_matrix, 0, 0f, 0f, 3f);
            Matrix.translateM(model_view_matrix, 0, sx, sy, 0f);
            Matrix.scaleM(model_view_matrix, 0, .2f, .2f, 1f);
            Matrix.translateM(model_view_matrix, 0, -.5f, 0f, 0f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // x arrow
                GLES20.glUniform4f(color_uniform, 0f, 1f, 1f, 1f);

                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferArrow);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferArrow.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }

            Matrix.translateM(model_view_matrix, 0, .5f, -.5f, 0f);
            Matrix.rotateM(model_view_matrix, 0, 90f, 0f, 0f, 1f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // y arrow
                GLES20.glUniform4f(color_uniform, 1f, 0f, 1f, 1f);

                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferArrow);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferArrow.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }

            Matrix.setIdentityM(model_view_matrix, 0);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);
        }

        {
            GLES20.glUseProgram(orbit_program);

            int position_attrib = GLES20.glGetAttribLocation(orbit_program, "aPosition");
            int color_uniform = GLES20.glGetUniformLocation(orbit_program, "uColor");
            int model_view_uniform = GLES20.glGetUniformLocation(orbit_program, "uModelView");
            int time_uniform = GLES20.glGetUniformLocation(orbit_program, "uTime");
            int tap_uniform = GLES20.glGetUniformLocation(orbit_program, "uTap");
            int orbit_uniform = GLES20.glGetUniformLocation(orbit_program, "uOrbit");
            int mu_uniform = GLES20.glGetUniformLocation(orbit_program, "uMu");

            float radius = (float)Math.sqrt(sx*sx+sy*sy);
            initial_phase = (float)Math.atan2(sy, sx);
            eccentricity = latus_rectum/radius-1;
            float mu = 10;
            float orbital_energy = mu*(eccentricity*eccentricity-1)/latus_rectum;
            float moment = (float)Math.sqrt(mu*latus_rectum);
            //Log.i("Orbit", "e=" + orbital_energy);
            GLES20.glUniform1f(time_uniform, current_time);
            GLES20.glUniform2f(tap_uniform, sx, sy);
            GLES20.glUniform3f(orbit_uniform, latus_rectum, eccentricity, initial_phase);
            GLES20.glUniform1f(mu_uniform, mu);

            float model_view_matrix[] = new float[16];
            Matrix.setIdentityM(model_view_matrix, 0);
            Matrix.translateM(model_view_matrix, 0, 0f, 0f, 1f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // orbit
                GLES20.glUniform4f(color_uniform, 0f, 0f, 0f, 1f);

                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferCircle);
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 1, verticesBufferCircle.capacity() / 4 - 1);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);

        {
            GLES20.glUseProgram(button_program);

            int position_attrib = GLES20.glGetAttribLocation(button_program, "aPosition");
            int color_uniform = GLES20.glGetUniformLocation(button_program, "uColor");
            int model_view_uniform = GLES20.glGetUniformLocation(button_program, "uModelView");
            int mode_uniform = GLES20.glGetUniformLocation(button_program, "uMode");

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);

            float model_view_matrix[] = new float[16];
            Matrix.setIdentityM(model_view_matrix, 0);
            Matrix.scaleM(model_view_matrix, 0, button_size/2, button_size/2, 0f);
            Matrix.translateM(model_view_matrix, 0, 1f, 1f, 0f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);


            { // left button
                GLES20.glUniform1i(mode_uniform, turn_left ? 1 : 0);
                GLES20.glUniform4f(color_uniform, 1f, 0f, 0f, 1f);

                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferSquare);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferSquare.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }

            Matrix.translateM(model_view_matrix, 0, 2f, 0f, 0f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // right button
                GLES20.glUniform1i(mode_uniform, turn_right ? 1 : 0);
                GLES20.glUniform4f(color_uniform, 0f, 1f, 0f, 1f);


                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferSquare);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferSquare.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }

            Matrix.setIdentityM(model_view_matrix, 0);
            Matrix.translateM(model_view_matrix, 0, width, 0f, 0f);
            Matrix.scaleM(model_view_matrix, 0, button_size/2, button_size/2, 0f);
            Matrix.translateM(model_view_matrix, 0, -1f, 1f, 0f);
            GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

            { // burn button
                GLES20.glUniform1i(mode_uniform, burn ? 1 : 0);
                GLES20.glUniform4f(color_uniform, 0f, 0f, 1f, 1f);


                GLES20.glEnableVertexAttribArray(position_attrib);
                GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferSquare);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferSquare.capacity() / 4);
                GLES20.glDisableVertexAttribArray(position_attrib);
            }
        }


        assertStatus();
    }

    @Override
    public void onSurfaceCreated(GL10 foo, EGLConfig config) {
        Log.i("Orbit", "create surface");

        { // texture
            textures = new int[2];
            GLES20.glGenTextures(textures.length, textures, 0);
            loadTexture(context.getResources().openRawResource(R.drawable.osb), textures[0]);
            loadTexture(context.getResources().openRawResource(R.drawable.button), textures[1]);
        }

        { // orbit shader
            int vertex_shader_id = loadShader(GLES20.GL_VERTEX_SHADER, context.getResources().getString(R.string.orbit_vertex_shader));
            int fragment_shader_id = loadShader(GLES20.GL_FRAGMENT_SHADER, context.getResources().getString(R.string.orbit_fragment_shader));
            orbit_program = GLES20.glCreateProgram();
            GLES20.glAttachShader(orbit_program, vertex_shader_id);
            GLES20.glAttachShader(orbit_program, fragment_shader_id);
            GLES20.glLinkProgram(orbit_program);
        }

        { // button shader
            int vertex_shader_id = loadShader(GLES20.GL_VERTEX_SHADER, context.getResources().getString(R.string.button_vertex_shader));
            int fragment_shader_id = loadShader(GLES20.GL_FRAGMENT_SHADER, context.getResources().getString(R.string.button_fragment_shader));
            button_program = GLES20.glCreateProgram();
            GLES20.glAttachShader(button_program, vertex_shader_id);
            GLES20.glAttachShader(button_program, fragment_shader_id);
            GLES20.glLinkProgram(button_program);
        }

        { // main shader
            int vertex_shader_id = loadShader(GLES20.GL_VERTEX_SHADER, context.getResources().getString(R.string.main_vertex_shader));
            int fragment_shader_id = loadShader(GLES20.GL_FRAGMENT_SHADER, context.getResources().getString(R.string.main_fragment_shader));
            main_program = GLES20.glCreateProgram();
            GLES20.glAttachShader(main_program, vertex_shader_id);
            GLES20.glAttachShader(main_program, fragment_shader_id);
            GLES20.glLinkProgram(main_program);
        }

        assertStatus();
    }
    @Override
    public void onSurfaceChanged(GL10 foo, int width_, int height_) {
        width = width_;
        height = height_;

        Log.i("Orbit", String.format("surface changed %d %d", width, height));
        GLES20.glViewport(0, 0, width, height);

        setProjection(main_program, true);
        setProjection(orbit_program, true);
        setProjection(button_program, false);
    }

    public void setProjection(int program, boolean physical_referential) {
        GLES20.glUseProgram(program);

        int projection_uniform = GLES20.glGetUniformLocation(program, "uProjection");
        if (projection_uniform >= 0) {
            float projection_matrix[] = new float[16];
            if (physical_referential) {
                float mx = width / (float) Math.min(height, width);
                float my = height / (float) Math.min(height, width);
                Matrix.orthoM(projection_matrix, 0, -mx, mx, -my, my, -10, 10);
            }
            else Matrix.orthoM(projection_matrix, 0, 0, width, 0, height, -10, 10);
            assertStatus();
            GLES20.glUniformMatrix4fv(projection_uniform, 1, false, projection_matrix, 0);
        }

        int size_uniform = GLES20.glGetUniformLocation(program, "uSize");
        if (size_uniform >= 0) GLES20.glUniform2f(size_uniform, width, height);

        assertStatus();
    }
}
