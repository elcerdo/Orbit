package com.example.pierre.orbit;


import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements Renderer {
    private FloatBuffer verticesBufferSquare;
    private FloatBuffer verticesBufferArrow;

    private float verticesArraySquare[] = {
      -1,-1,0,1,
      1,-1,0,1,
      1,1,0,1,
      -1,1,0,1,
    };

    private float verticesArrayArrow[] = {
      1,0,0,1,
      .9f,.1f,0,1,
      .9f,.05f,0,1,
      0,.05f,0,1,
      0,-.05f,0,1,
      .9f,-.05f,0,1,
      .9f,-.1f,0,1,
    };

    public int counter;

    private String vertexShaderCode =
            "precision highp float;" +
            "attribute vec4 vPosition;" +
            "uniform mat4 uModelView;" +
            "uniform mat4 uProjection;" +
            "void main() {" +
            "  gl_Position = uProjection*uModelView*vPosition;" +
            "}";
    private String fragmentShaderCode =
            "precision highp float;" +
            "uniform vec4 uColor;" +
            "void main() {" +
            "  gl_FragColor = uColor;" +
            "}";
    private int programId;


    public static FloatBuffer arrayToBuffer(float array[]) {
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(array.length * 4);
        byte_buffer.order(ByteOrder.nativeOrder());
        FloatBuffer float_buffer = byte_buffer.asFloatBuffer();
        float_buffer.put(array);
        float_buffer.position(0);
        return float_buffer;
    }

    public static int loadShader(int type, String code) {
        int shaderId = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shaderId, code);
        GLES20.glCompileShader(shaderId);
        if (!logStatus()) Log.e("Orbit", "COMPILE ERROR " + GLES20.glGetShaderInfoLog(shaderId));
        return shaderId;
    }

    public static boolean logStatus() {
        int error = GLES20.glGetError();
        if (error == GLES20.GL_NO_ERROR) return true;
        Log.e("Orbit", "STATUS ERROR " + GLU.gluErrorString(error));
        return false;
    }

    MainRenderer() {
        Log.i("Orbit", "create renderer");
        counter = 2;
        verticesBufferSquare = arrayToBuffer(verticesArraySquare);
        verticesBufferArrow = arrayToBuffer(verticesArrayArrow);
    }

    @Override
    public void onDrawFrame(GL10 foo) {
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) return;

        GLES20.glClearColor(1,1,1,1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(programId);

        int position_attrib = GLES20.glGetAttribLocation(programId, "vPosition");
        int color_uniform = GLES20.glGetUniformLocation(programId, "uColor");
        int model_view_uniform = GLES20.glGetUniformLocation(programId, "uModelView");
        assert( position_attrib >= 0 );
        assert( color_uniform >= 0 );
        assert( model_view_uniform >= 0 );

        float model_view_matrix[] = new float[16];
        Matrix.setIdentityM(model_view_matrix, 0);
        Matrix.scaleM(model_view_matrix, 0, .5f, .5f, 1.f);
        GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

        GLES20.glUniform4f(color_uniform, counter % 3 == 0 ? 1.f : 0.f, counter % 3 == 1 ? 1.f : 0.f, counter % 3 == 2 ? 1.f : 0.f, 1.f);

        GLES20.glEnableVertexAttribArray(position_attrib);
        GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferSquare);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(position_attrib);

        Matrix.translateM(model_view_matrix, 0, -1f,-1f,0f);
        Matrix.scaleM(model_view_matrix, 0, 2f, 2f, 1f);
        GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

        GLES20.glUniform4f(color_uniform, 1f,0f,0f,1f);

        GLES20.glEnableVertexAttribArray(position_attrib);
        GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferArrow);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 7);
        GLES20.glDisableVertexAttribArray(position_attrib);

        Matrix.rotateM(model_view_matrix, 0, 90f, 0f,0f,1f);
        GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

        GLES20.glUniform4f(color_uniform, 0f,1f,0f,1f);

        GLES20.glEnableVertexAttribArray(position_attrib);
        GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferArrow);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 7);
        GLES20.glDisableVertexAttribArray(position_attrib);

        logStatus();
    }
    @Override
    public void onSurfaceCreated(GL10 foo, EGLConfig config) {
        Log.i("Orbit", "create surface");


        {
            int vertex_shader_id = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragment_shader_id = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            programId = GLES20.glCreateProgram();
            GLES20.glAttachShader(programId, vertex_shader_id);
            GLES20.glAttachShader(programId, fragment_shader_id);
            GLES20.glLinkProgram(programId);
        }

        logStatus();
    }
    @Override
    public void onSurfaceChanged(GL10 foo, int width, int height) {
        Log.i("Orbit", String.format("surface changed %d %d", width, height));

        GLES20.glUseProgram(programId);

        int projection_uniform = GLES20.glGetUniformLocation(programId, "uProjection");
        assert( projection_uniform >= 0 );

        float projection_matrix[] = new float[16];
        float mx = width/(float)Math.min(height, width);
        float my = height/(float)Math.min(height, width);
        Matrix.orthoM(projection_matrix, 0, -mx,mx,-my,my,-1,1);

        GLES20.glUniformMatrix4fv(projection_uniform, 1, false, projection_matrix, 0);
    }
}
