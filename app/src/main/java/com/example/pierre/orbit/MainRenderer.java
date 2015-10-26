package com.example.pierre.orbit;


import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements Renderer {
    private FloatBuffer verticesBuffer;

    private float vertices[] = {
      -1,-1,0,1,
      1,-1,0,1,
      1,1,0,1,
      -1,1,0,1,
    };

    public int counter;

    private String vertexShaderCode =
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = mat4(.5,0,0,0, 0,.5,0,0, 0,0,1,0, 0,0,0,1)*vPosition;" +
            "}";
    private String fragmentShaderCode =
            "precision highp float;" +
            "uniform vec4 uColor;" +
            "void main() {" +
            "  gl_FragColor = uColor;" +
            "}";
    private int programId;


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
        Log.e("Orbit", "ERROR " + GLU.gluErrorString(error));
        return false;
    }

    @Override
    public void onDrawFrame(GL10 foo) {
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) return;

        GLES20.glClearColor(1,0,0,1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(programId);

        int position_attrib = GLES20.glGetAttribLocation(programId, "vPosition");
        int color_uniform = GLES20.glGetUniformLocation(programId, "uColor");

        GLES20.glUniform4f(color_uniform, counter%3 == 0 ? 1.f : 0.f, counter%3 == 1 ? 1.f : 0.f, counter %3 == 2 ? 1.f : 0.f, 1.f);

        GLES20.glEnableVertexAttribArray(position_attrib);
        GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(position_attrib);

        logStatus();
    }
    @Override
    public void onSurfaceCreated(GL10 foo, EGLConfig config) {
        Log.i("Orbit", "surfaceCreated");

        {
            ByteBuffer vertices_bytes_buffer = ByteBuffer.allocateDirect(vertices.length * 4);
            vertices_bytes_buffer.order(ByteOrder.nativeOrder());
            verticesBuffer = vertices_bytes_buffer.asFloatBuffer();
            verticesBuffer.put(vertices);
            verticesBuffer.position(0);
        }

        {
            int vertex_shader_id = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragment_shader_id = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            programId = GLES20.glCreateProgram();
            GLES20.glAttachShader(programId, vertex_shader_id);
            GLES20.glAttachShader(programId, fragment_shader_id);
            GLES20.glLinkProgram(programId);
        }

        counter = 2;
        logStatus();
    }
    @Override
    public void onSurfaceChanged(GL10 foo, int aa, int bb) {

    }
}
