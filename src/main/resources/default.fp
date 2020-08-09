#version 460

out vec4 fragColor;

in PerVertexData {
    vec4 color;
} fragIn;

void main() {
    fragColor = fragIn.color;
}