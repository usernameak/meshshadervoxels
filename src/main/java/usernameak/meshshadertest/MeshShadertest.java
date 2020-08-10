package usernameak.meshshadertest;

import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MeshShadertest {
    public static int createShader(int type, String path) {
        int handle = GL20.glCreateShader(type);

        try {
            GL20.glShaderSource(handle, IOUtils.toString(MeshShadertest.class.getResourceAsStream(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GL20.glCompileShader(handle);

        if (GL20.glGetShaderi(handle, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            throw new RuntimeException("error compiling " + path + ":" + GL20.glGetShaderInfoLog(handle));
        }
        checkGLError();
        return handle;
    }

    private static void worldGen(IntBuffer buffer) {
        PerlinGenerator perlinGenerator = new PerlinGenerator(new Random().nextLong(), 3, 8, 5.0, true);
        for (int i = 0; i < 32; i++) {
            for (int k = 0; k < 32; k++) {
                double fh = perlinGenerator.generate(i / 0.16, k / 0.16);
                int h = (int) (fh * 0.32);
                if (h < 0) {
                    h = 0;
                }
                if (h >= 32) {
                    h = 32;
                }
                for (int j = 0; j < h; j++) {
                    buffer.put(i + j * 1024 + k * 32, h);
                }
            }
        }
        buffer.clear();
    }

    private static void checkGLError() {
        int error = GL11.glGetError();
        if(error == GL11.GL_NO_ERROR) {
            System.err.println(error);
            new Exception().printStackTrace();
        }
    }

    public static void main(String[] args) {
        GLFW.glfwInit();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 4);
        long window = GLFW.glfwCreateWindow(768, 1024, "mesh shader test", 0L, 0L);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        System.out.println("CPU: " + GL11.glGetString(GL11.GL_RENDERER));
        // System.out.println("max workgroup size: " + GL11.glGetInteger(GL_MAX_MESH_WORK_GROUP_SIZE_NV));

        int program;
        {
            int taskShader = createShader(NVMeshShader.GL_TASK_SHADER_NV, "/default.tp");
            int meshShader = createShader(NVMeshShader.GL_MESH_SHADER_NV, "/default.mp");
            int fragmentShader = createShader(GL20.GL_FRAGMENT_SHADER, "/default.fp");
            program = createProgram(taskShader, meshShader, fragmentShader);
        }

        int ssbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        IntBuffer buf = BufferUtils.createIntBuffer(65536);
        worldGen(buf);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buf, GL15.GL_STATIC_DRAW);
        checkGLError();

        int fpsc = 0;
        long fpsNanoCounter = 0;
        long fpsNanoCounterLast = System.nanoTime();
        int fps = 0;

        while (!GLFW.glfwWindowShouldClose(window)) {
            int[] width_ = new int[1];
            int[] height_ = new int[1];
            GLFW.glfwGetFramebufferSize(window, width_, height_);
            int width = width_[0];
            int height = height_[0];

            GL11.glViewport(0, 0, width, height);

            GL11.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            checkGLError();

            GL11.glEnable(GL11.GL_DEPTH_TEST);

            Matrix4f projectionMatrix = new Matrix4f();
            projectionMatrix.perspective(0.90f, (float) width / (float) height, 1.0f, 0.1f);
            Matrix4f viewMatrix = new Matrix4f();
            viewMatrix.lookAt(0.48f, 0.48f, 0.48f, 0.0f, 0.0f, 0.0f, 0.0f, 0.1f, 0.0f);

            Matrix4f mvpMatrix = new Matrix4f().mul(projectionMatrix).mul(viewMatrix);
            GL20.glUseProgram(program);
            FloatBuffer fb = BufferUtils.createFloatBuffer(16);
            mvpMatrix.get(fb);
            GL20.glUniformMatrix4fv(0, false, fb);
            checkGLError();

            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssbo);
            checkGLError();

            NVMeshShader.glDrawMeshTasksNV(0, 32768);
            checkGLError();

            GLFW.glfwSwapBuffers(window);

            fpsc++;
            fpsNanoCounter = System.nanoTime();
            if (fpsNanoCounter >= fpsNanoCounterLast + 1000000000L) {
                fpsNanoCounterLast += 1000000000L;
                fps = fpsc;
                GLFW.glfwSetWindowTitle(window, fps + " FPS");
                fpsc = 0;
            }

            GLFW.glfwPollEvents();
        }
        GLFW.glfwTerminate();
    }

    private static int createProgram(int... shaders) {
        int handle = GL20.glCreateProgram();
        for (int shader : shaders) {
            GL20.glAttachShader(handle, shader);
        }
        GL20.glLinkProgram(handle);

        if (GL20.glGetProgrami(handle, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            throw new RuntimeException("error linking:" + GL20.glGetProgramInfoLog(handle));
        }
        checkGLError();
        return handle;
    }
}
