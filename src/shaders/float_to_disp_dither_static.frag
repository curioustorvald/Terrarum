#version 400
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_pattern;


uniform vec4 topColor;
uniform vec4 bottomColor;
uniform float parallax = 0.0; // +1.0: all top col, -1.0: all bototm col, 0.0: normal grad
uniform float parallax_size = 1.0/3.0; // 0: no parallax

// inverted zoom; this value must set to (1f/zoom)
uniform float zoomInv = 1.0;

out vec4 fragColor;

const float quant = 255.0; // 64 steps -> 63.0; 256 steps -> 255.0
const vec4 quantiser = vec4(quant);
const vec4 quantiserDivider = vec4(1.0 / quant);

const vec2 boolean = vec2(0.0, 1.0);
const vec4 halfvec = vec4(0.5);

const vec2 patternsize = vec2(1.0/512.0, 1.0/512.0);

vec4 nearestColour(vec4 inColor) {
    return floor(quantiser * inColor + halfvec) * quantiserDivider;
}

vec4 getDitherredDot(vec4 inColor) {
    vec4 bayerThreshold = vec4(texture(u_pattern, gl_FragCoord.xy * patternsize) - 0.5);
    return nearestColour(inColor + bayerThreshold * quantiserDivider);
}


void main(void) {
    float parallaxAdder = 0.5 * (parallax + 1.0) * parallax_size;
    float scale = fma(v_texCoords.y, 1.0 - parallax_size, parallaxAdder);

    float zoomSamplePoint = (1.0 - zoomInv) / 2.0;// will never quite exceed 0.5

    // I don't even know if it works, and also not sure if I actually want it
    vec4 newBottom = mix(bottomColor, topColor, zoomSamplePoint);
    vec4 newTop = mix(topColor, bottomColor, zoomSamplePoint);

    vec4 inColor = v_color * mix(newBottom, newTop, scale);
    vec4 selvec = getDitherredDot(inColor);

    fragColor = selvec;
}

/*
UV mapping coord.y

-+ <- 1.0  =
D|         = // parallax of +1
i|  =      =
s|  = // parallax of 0
p|  =      =
.|         = // parallax of -1
-+ <- 0.0  =
*/