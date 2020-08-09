#version 460

#extension GL_NV_mesh_shader : require
#extension GL_NV_shader_thread_group : require
#extension GL_NV_gpu_shader5 : require

layout(local_size_x = 6) in;
layout(triangles, max_vertices = 6, max_primitives = 2) out;
//layout(points, max_vertices = 1, max_primitives = 1) out;

taskNV in Task {
    uint      block_position;
    uint8_t   faceIds[6];
} IN;

out PerVertexData {
    vec4 color;
} v_out[];

const vec3 cubeFaceVertices[6][6] = {
    {
        vec3(1.0, 0.0, 0.0),
        vec3(1.0, 0.0, 1.0),
        vec3(0.0, 0.0, 1.0),
        vec3(0.0, 0.0, 1.0),
        vec3(0.0, 0.0, 0.0),
        vec3(1.0, 0.0, 0.0)
    },
    {
        vec3(0.0, 1.0, 0.0),
        vec3(0.0, 1.0, 1.0),
        vec3(1.0, 1.0, 1.0),
        vec3(1.0, 1.0, 1.0),
        vec3(1.0, 1.0, 0.0),
        vec3(0.0, 1.0, 0.0)
    },
    {
        vec3(0.0, 1.0, 0.0),
        vec3(1.0, 1.0, 0.0),
        vec3(1.0, 0.0, 0.0),
        vec3(1.0, 0.0, 0.0),
        vec3(0.0, 0.0, 0.0),
        vec3(0.0, 1.0, 0.0)
    },
    {
        vec3(0.0, 0.0, 1.0),
        vec3(1.0, 0.0, 1.0),
        vec3(1.0, 1.0, 1.0),
        vec3(1.0, 1.0, 1.0),
        vec3(0.0, 1.0, 1.0),
        vec3(0.0, 0.0, 1.0)
    },
    {
        vec3(0.0, 0.0, 0.0),
        vec3(0.0, 0.0, 1.0),
        vec3(0.0, 1.0, 1.0),
        vec3(0.0, 1.0, 1.0),
        vec3(0.0, 1.0, 0.0),
        vec3(0.0, 0.0, 0.0)
    },
    {
        vec3(1.0, 1.0, 0.0),
        vec3(1.0, 1.0, 1.0),
        vec3(1.0, 0.0, 1.0),
        vec3(1.0, 0.0, 1.0),
        vec3(1.0, 0.0, 0.0),
        vec3(1.0, 1.0, 0.0)
    }
};

layout(location = 0) uniform mat4 modelViewProjection;

void main() {
    uint position = IN.block_position;
    uint8_t face = IN.faceIds[gl_WorkGroupID.x];
    uint vertexId = gl_LocalInvocationID.x;
    float x = float(position % 16);
    float y = float((position / 16) % 16);
    float z = float(position / 256);
    vec3 blockPos = vec3(x, y, z);
    gl_MeshVerticesNV[vertexId].gl_Position = modelViewProjection * vec4(cubeFaceVertices[face][vertexId] + blockPos, 1.0);
    v_out[vertexId].color = vec4(blockPos / 16.0, 1.0);
    // gl_MeshVerticesNV[vertexId].gl_Position = modelViewProjection * vec4(blockPos, 1.0);
    gl_PrimitiveIndicesNV[vertexId] = vertexId;
    gl_PrimitiveCountNV = 2;
    // gl_PrimitiveCountNV = 1;
}