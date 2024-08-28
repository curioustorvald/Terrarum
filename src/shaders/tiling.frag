
#ifdef GL_ES
precision mediump float;
#endif

layout(origin_upper_left,pixel_center_integer) in vec4 gl_FragCoord; // is now top-down and strictly integer

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;


uniform vec2 tilesInAxes; // 8x8

uniform sampler2D tilemap; // RGBA8888
uniform sampler2D tilemap2; // RGBA8888
uniform sampler2D tilesAtlas; // terrain, wire, fluids, etc.
uniform sampler2D tilesBlendAtlas; // alternative terrain for the weather mix (e.g. yellowed grass)
uniform sampler2D deblockingMap;

uniform float tilesBlend = 0.0; // percentage of blending [0f..1f]. 0: draws tilesAtlas, 1: draws tilesBlendAtlas

uniform vec2 tilesInAtlas = vec2(256.0, 256.0);
uniform vec2 atlasTexSize = vec2(2048.0, 2048.0);

vec2 _tilesInAtlas = vec2(1.0, 1.0) / tilesInAtlas;
vec2 tileSizeInPx = atlasTexSize * _tilesInAtlas; // should be like ivec2(8.0, 8.0)
vec2 _tileSizeInPx = vec2(1.0, 1.0) / tileSizeInPx; // should be like ivec2(0.125, 0.125)

uniform vec4 colourFilter = vec4(1, 1, 1, 1); // used by WALL to darken it

uniform ivec2 cameraTranslation = ivec2(0, 0); // used to offset the drawing; it's integer because we want the drawing to be pixel-aligned

uniform float drawBreakage = 1.0; // set it to 0f to not draw breakage, 1f to draw it; NEVER set to any other values.

uniform float mulBlendIntensity = 1.0; // used my MUL-blending drawings; works about the same way as the Layer Opacity slider of Photoshop/Krita/etc.

const vec2 bc = vec2(1.0, 0.0); //binary constant
const vec2 haalf = vec2(0.5, 0.5);

out vec4 fragColor;

ivec4 tileNumberToXY(int tileNumber) {
    int tileX = tileNumber % int(tilesInAtlas.x);
    int tileY = tileNumber / int(tilesInAtlas.x);
    return ivec4(
        tileX, // tileX
        tileY, // tileY
        tileX % 2, // quadrant-x (0, 1)
        tileY % 2 // quadrant-y (0, 1)
    );
}

// return: ivec3(tileID, breakage, fliprot)
ivec3 _colToInt(vec4 map1, vec4 map2) {
    ivec3 col1 = ivec3(
        int(map1.r * 255),
        int(map1.g * 255),
        int(map1.b * 255));

    ivec3 col2 = ivec3(
        int(map2.r * 255),
        int(map2.g * 255),
        int(map2.b * 255));

    return ivec3(
        (col1.r << 16) | (col1.g << 8) | col1.b, // tile
        col2.b, // breakage
        col2.g // fliprot
    );
}

mat3x2[] flipRotMat = mat3x2[](
mat3x2( 1.0,  0.0,  0.0,  1.0, tileSizeInPx.x*0.0, tileSizeInPx.y*0.0),
mat3x2(-1.0,  0.0,  0.0,  1.0, tileSizeInPx.x*1.0, tileSizeInPx.y*0.0),
mat3x2( 0.0, -1.0,  1.0,  0.0, tileSizeInPx.x*0.0, tileSizeInPx.y*1.0),
mat3x2( 0.0,  1.0,  1.0,  0.0, tileSizeInPx.x*0.0, tileSizeInPx.y*0.0),
mat3x2(-1.0,  0.0,  0.0, -1.0, tileSizeInPx.x*1.0, tileSizeInPx.y*1.0),
mat3x2( 1.0,  0.0,  0.0, -1.0, tileSizeInPx.x*0.0, tileSizeInPx.y*1.0),
mat3x2( 0.0,  1.0, -1.0,  0.0, tileSizeInPx.x*1.0, tileSizeInPx.y*0.0),
mat3x2( 0.0, -1.0, -1.0,  0.0, tileSizeInPx.x*1.0, tileSizeInPx.y*1.0)
);

vec2 uvFlipRot(int op, vec2 uv) {
    return (flipRotMat[op] * vec3(uv, 1.0)).xy;
}

const vec4 _four = vec4(1.0 / 4.0);
const vec4 _three = vec4(1.0 / 3.0);
const vec4 _two = vec4(1.0 / 2.0);
const vec4 zero = vec4(0.0);
const float blur = 1.0;
vec2 blurU = vec2(0.0, -blur);
vec2 blurD = vec2(0.0, +blur);
vec2 blurL = vec2(-blur, 0.0);
vec2 blurR = vec2(+blur, 0.0);

vec2 overscannedScreenDimension = tilesInAxes * tileSizeInPx; // how many tiles will fit into a screen; one used by the tileFromMap; we need this because screen size is not integer multiple of the tile size

vec4[2] getTileNumbersFromMap(vec2 fragCoord) {
    vec4 tileFromMap = texture(tilemap, fragCoord / overscannedScreenDimension); // raw tile number
    vec4 tileFromMap2 = texture(tilemap2, fragCoord / overscannedScreenDimension); // raw tile number
    return vec4[2](tileFromMap, tileFromMap2);
}

vec4 getBlurmapNumbersFromMap(vec2 fragCoord) {
    return texture(deblockingMap, fragCoord / overscannedScreenDimension);// raw tile number
}

vec4[3] getFragColorForOnscreenCoord(vec2 fragCoord) {
    vec4[] tileFromMap = getTileNumbersFromMap(fragCoord);
    ivec3 tbf = _colToInt(tileFromMap[0], tileFromMap[1]);
    int tile = tbf.x;
    int breakage = tbf.y;
    int flipRot = tbf.z;
    ivec4 tileXYnQ = tileNumberToXY(tile);
    ivec2 tileXY = tileXYnQ.xy;
    ivec2 tileQ = tileXYnQ.zw;
    ivec2 breakageXY = tileNumberToXY(2*(breakage + 5)).xy; // +5 is hard-coded constant that depends on the contents of the atlas

    vec4 blurFromMap = getBlurmapNumbersFromMap(fragCoord);
    ivec3 tbf2 = _colToInt(blurFromMap, zero);
    int blurTileNum = tbf2.x;
    ivec4 blurXYnQ = tileNumberToXY(blurTileNum);
    ivec2 blurXY = blurXYnQ.xy;

    // calculate the UV coord value for texture sampling //

    // don't really need highp here; read the GLES spec
    vec2 uvCoordForTile = uvFlipRot(flipRot, mod(fragCoord, tileSizeInPx)) * _tileSizeInPx * _tilesInAtlas; // 0..0.00390625 regardless of tile position in atlas
    vec2 uvCoordForTile1 = mod(fragCoord, tileSizeInPx) * _tileSizeInPx * _tilesInAtlas;// 0..0.00390625 regardless of tile position in atlas
    vec2 uvCoordOffsetTile = tileXY * _tilesInAtlas; // where the tile starts in the atlas, using uv coord (0..1)
    vec2 uvCoordOffsetBreakage = (breakageXY + tileQ) * _tilesInAtlas;
    vec2 uvCoordOffsetBlurmap = blurXY * _tilesInAtlas;

    // get final UV coord for the actual sampling //

    vec2 finalUVCoordForTile = uvCoordForTile + uvCoordOffsetTile;// where we should be actually looking for in atlas, using UV coord (0..1)
    vec2 finalUVCoordForBreakage = uvCoordForTile1 + uvCoordOffsetBreakage;
    vec2 finalUVCoordForBlurmap = uvCoordForTile1 + uvCoordOffsetBlurmap;

    // blending a breakage tex with main tex //

    vec4 tileCol = texture(tilesAtlas, finalUVCoordForTile);
    vec4 tileAltCol = texture(tilesBlendAtlas, finalUVCoordForTile);

    return vec4[](
        mix(tileCol, tileAltCol, tilesBlend),
        texture(tilesAtlas, finalUVCoordForBreakage),
        texture(tilesAtlas, finalUVCoordForBlurmap)
    );
}

vec4 getFragColorForOnscreenCoord1(vec2 fragCoord) {
    vec4[] tileFromMap = getTileNumbersFromMap(fragCoord);
    ivec3 tbf = _colToInt(tileFromMap[0], tileFromMap[1]);
    int tile = tbf.x;
    int breakage = tbf.y;
    int flipRot = tbf.z;
    ivec4 tileXYnQ = tileNumberToXY(tile);
    ivec2 tileXY = tileXYnQ.xy;
    ivec2 tileQ = tileXYnQ.zw;

    // calculate the UV coord value for texture sampling //

    // don't really need highp here; read the GLES spec
    vec2 uvCoordForTile = uvFlipRot(flipRot, mod(fragCoord, tileSizeInPx)) * _tileSizeInPx * _tilesInAtlas; // 0..0.00390625 regardless of tile position in atlas
    vec2 uvCoordOffsetTile = tileXY * _tilesInAtlas; // where the tile starts in the atlas, using uv coord (0..1)

    // get final UV coord for the actual sampling //

    vec2 finalUVCoordForTile = uvCoordForTile + uvCoordOffsetTile;// where we should be actually looking for in atlas, using UV coord (0..1)

    // blending a breakage tex with main tex //

    vec4 tileCol = texture(tilesAtlas, finalUVCoordForTile);
    vec4 tileAltCol = texture(tilesBlendAtlas, finalUVCoordForTile);

    return mix(tileCol, tileAltCol, tilesBlend);
}

void main() {
    vec2 fragCoord = gl_FragCoord.xy + cameraTranslation + haalf; // manually adding half-int to the flipped gl_FragCoord: this avoids driver bug present on the Asahi Linux and possibly (but unlikely) others

    vec4[] tile_breakage_blur = getFragColorForOnscreenCoord(fragCoord);

    vec4 tileC = tile_breakage_blur[0];
    vec4 tileL = getFragColorForOnscreenCoord1(fragCoord + blurL);
    vec4 tileR = getFragColorForOnscreenCoord1(fragCoord + blurR);
    vec4 tileU = getFragColorForOnscreenCoord1(fragCoord + blurU);
    vec4 tileD = getFragColorForOnscreenCoord1(fragCoord + blurD);

    vec4 blurH = (tileC + tileC + tileL + tileR) * _four;
    vec4 blurV = (tileC + tileC + tileU + tileD) * _four;
    vec4 blurPower = tile_breakage_blur[2];

    vec4 finalTile = mix(
        mix(tileC, blurH, blurPower.x),
        mix(tileC, blurV, blurPower.y),
        0.5
    );

    vec4 finalBreakage = drawBreakage * tile_breakage_blur[1]; // drawBreakeage = 0 to not draw, = 1 to draw

    vec4 finalColor = fma(mix(finalTile, finalBreakage, finalBreakage.a), bc.xxxy, finalTile * bc.yyyx);

    fragColor = mix(colourFilter, colourFilter * finalColor, mulBlendIntensity);
//    fragColor = blurPower;
}
