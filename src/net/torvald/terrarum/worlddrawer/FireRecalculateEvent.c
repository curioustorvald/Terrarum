#include <xmmintrin.h>
#include <jni.h>
#include "net_torvald_terrarum_worlddrawer_LightmapRenderer.h"

/*
compile required headers with:
"C:\Program Files\Java\jdk1.8.0_131\bin\javah.exe" -jni -cp "./lib/gdx.jar;C:\Users\minjaesong\.IdeaIC2018.3\config\plugins\JBSDKDownloadHelper\lib\kotlin-runtime.jar;C:\Users\minjaesong\Documents\terr
arum\build\libs\Terrarum-0.2.jar" net.torvald.terrarum.worlddrawer.LightmapRenderer

get method signatures with:
"C:\Program Files\Java\jdk1.8.0_131\bin\javap.exe" -s -p -cp "./lib/gdx.jar;C:\Users\minjaesong\.IdeaIC2018.3\config\plugins\JBSDKDownloadHelper\lib\kotlin-runtime.jar;C:\Users\minjaesong\Documents\ter
rarum\build\libs\Terrarum-0.2.jar" net.torvald.terrarum.gameworld.GameWorld
*/

#define vec4 __m128
#define overscan_open net_torvald_terrarum_worlddrawer_LightmapRenderer_overscan_open
#define DIV_FLOAT net_torvald_terrarum_worlddrawer_LightmapRenderer_DIV_FLOAT


typedef struct {
    int type;
    float fill;
} FluidInfo;

vec4 lightLevelThis;
jobject thisTerrain, thisWall, thisFluid;
vec4 fluidAmountToCol;
vec4 thisTileLuminosity, thisTileOpacity, thisTileOpacity2, sunLight;

const vec4 lightMagic = _mm_set1_ps(8);
const vec4 oneVec = _mm_set1_ps(1);
const vec4 zeroVec = _mm_set1_ps(0);
const vec4 sqrt2Vec = _mm_set1_ps(1.41421356f);

int LIGHTMAP_WIDTH;
int for_x_start = 0;
int for_y_start = 0;
int for_x_end = 0;
int for_y_end = 0;

void getLightsAndShades(JNIEnv *env, jclass cls, jobject obj, int x, int y) {
    // get world first
    jfieldID fid = (*env)->GetFieldID(env, cls, "world", "Lnet/torvald/terrarum/gameworld/GameWorld;");
    jobject worldobj = (*env)->GetObjectField(env, obj, fid);
    jclass worldcls = (*env)->GetObjectClass(env, worldobj);

    // get method IDs
    jmethodID getTerrain_mid = (*env)->GetMethodID(env, worldcls, "getTileFromTerrain", "(II)Ljava/lang/Integer;")
    jmethodID getFluid_mid = (*env)->GetMethodID(env, worldcls, "getFluid", "(II)Lnet/torvald/terrarum/gameworld/GameWorld$FluidInfo;")
    jmethodID getWall_mid = (*env)->GetMethodID(env, worldcls, "getTileFromWall", "(II)Ljava/lang/Integer;")
    jmethodID getSun_mid = (*env)->GetMethodID(env, worldcls, "getGlobalLight", "()Lcom/badlogic/gdx/graphics/Color;")

    // populate the vars
    thisTerrain = (*env)->CallObjectMethod(env, worldobj, getTerrain_mid, x, y);
    thisFluid = (*env)->CallObjectMethod(env, worldobj, getFluid_mid, x, y);
    thisWall = (*env)->CallObjectMethod(env, worldobj, getWall_mid, x, y);

    // set sunlight
    jobject sun_gdxcolor = (*env)->CallObjectMethod(env, worldobj, getSun_mid, x, y);
    jclass gdxcolor_cls = (*env)->GetObjectClass(env, sun_gdxcolor);
    jfieldID sunfid = (*env)->GetFieldID(env, gdxcolor_cls, "r", "F");
    float sunr = GetFloatField(env, sun_gdxcolor, sunfid) * DIV_FLOAT;
    sunfid = (*env)->GetFieldID(env, gdxcolor_cls, "g", "F");
    float sung = GetFloatField(env, sun_gdxcolor, sunfid) * DIV_FLOAT;
    sunfid = (*env)->GetFieldID(env, gdxcolor_cls, "b", "F");
    float sunb = GetFloatField(env, sun_gdxcolor, sunfid) * DIV_FLOAT;
    sunfid = (*env)->GetFieldID(env, gdxcolor_cls, "a", "F");
    float suna = GetFloatField(env, sun_gdxcolor, sunfid) * DIV_FLOAT;

    // TODO check rgba argument order (RGBA or ABGR, intel doc is confusing)
    sunLight = _mm_set_ps(sunr, sung, sunb, suna);

    // get fluid info
    jclass fluid_cls = (*env)->GetObjectClass(env, thisFluid);
    jfieldID fluidfid = (*env)->GetFieldID(env, fluid_cls, "type", "I");
    jint fluidType = GetIntField(env, thisFluid, fluidfid);
    fluidfid = (*env)->GetFieldID(env, fluid_cls, "amount", "F");
    jfloat fluidFill = GetIntField(env, thisFluid, fluidfid);


    lightLevelThis = _mm_set1_ps(0);

    if (fluidType != 0) {
        fluidAmountToCol = _mm_set1_ps(fluidFill);
        thisTileLuminosity = 
    }

}

vec4 _mm_darken_ps(vec4 data, vec4 darken) {
    // data * (1f - darken * lightMagic)
    return _mm_mul_ps(
        data,
        _mm_sub_ps(
            oneVec,
            mm_mul_ps(
                darken,
                lightMagic
            )
        )
    );
}

vec4 calculate(int x, int y) {
    getLightsAndShades(x, y);

    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x - 1, y - 1), thisTileOpacity2));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x + 1, y - 1), thisTileOpacity2));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x - 1, y + 1), thisTileOpacity2));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x + 1, y + 1), thisTileOpacity2));

    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x    , y - 1), thisTileOpacity));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x    , y + 1), thisTileOpacity));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x - 1, y    ), thisTileOpacity));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x + 1, y    ), thisTileOpacity));

    return lightLevelThis
}

void setLightOf(JNIEnv *env, jclass cls, int x, int y, vec4 colour) {
    if (y - for_y_start + overscan_open >= 0 &&
        y - for_y_start + overscan_open < LIGHTMAP_HEIGHT &&

        x - for_x_start + overscan_open >= 0 &&
        x - for_x_start + overscan_open < LIGHTMAP_WIDTH) {

        int ypos = y - for_y_start + overscan_open
        int xpos = x - for_x_start + overscan_open

        // unpack our vector
        float[4] vector;
        _mm_store_ps(vector *, colour);

        // get the array
        jfieldID fid = (*env)->GetFieldID(env, cls, "lightmap", "F");
        jfloat * list = (*env)->GetFloatArrayElements(env, jfloatarray, null);

        // set r/g/b/a values
        list[4 * (ypos * LIGHTMAP_WIDTH + xpos)] = vector[0];
        list[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 1] = vector[1];
        list[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 2] = vector[2];
        list[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 3] = vector[3];

        //list[4 * ypos * LIGHTMAP_WIDTH + xpos] = colour
    }
}

vec4 getLightInternal(JNIEnv *env, jobject obj, int x, int y) {
    if (y - for_y_start + overscan_open >= 0 &&
        y - for_y_start + overscan_open < LIGHTMAP_HEIGHT &&

        x - for_x_start + overscan_open >= 0 &&
        x - for_x_start + overscan_open < LIGHTMAP_WIDTH) {

        int ypos = y - for_y_start + overscan_open
        int xpos = x - for_x_start + overscan_open

        // get the array
        jclass cls = (*env)->GetObjectClass(env, obj);
        jfieldID fid = (*env)->GetFieldID(env, cls, "lightmap", "F");
        jfloat * list = (*env)->GetFloatArrayElements(env, jfloatarray, null);

        float r = list[4 * (ypos * LIGHTMAP_WIDTH + xpos)];
        float g = list[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 1];
        float b = list[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 2];
        float a = list[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 3];

        // TODO check rgba argument order (RGBA or ABGR, intel doc is confusing)
        return _mm_set_ps(r, g, b, a);
    }
}

JNIEXPORT void JNICALL Java_net_torvald_terrarum_worlddrawer_LightmapRenderer_fireRecalculateEventJNI(JNIEnv *env, jobject object) {
    // update (read) variables
    jclass cls = (*env)->GetObjectClass(env, object);
    jfieldID fid = (*env)->GetFieldID(env, cls, "LIGHTMAP_WIDTH", "I");
    LIGHTMAP_WIDTH = (*env)->GetIntField(env, obj, fid);
    fid = (*env)->GetFieldID(env, cls, "for_x_start", "I");
    for_x_start = (*env)->GetIntField(env, obj, fid);
    fid = (*env)->GetFieldID(env, cls, "for_y_start", "I");
    for_y_start = (*env)->GetIntField(env, obj, fid);
    fid = (*env)->GetFieldID(env, cls, "for_x_end", "I");
    for_x_end = (*env)->GetIntField(env, obj, fid);
    fid = (*env)->GetFieldID(env, cls, "for_y_end", "I");
    for_y_end = (*env)->GetIntField(env, obj, fid);


    /**
     * Updating order:
     * ,--------.   ,--+-----.   ,-----+--.   ,--------. -
     * |↘       |   |  |    3|   |3    |  |   |       ↙| ↕︎ overscan_open / overscan_opaque
     * |  ,-----+   |  |  2  |   |  2  |  |   +-----.  | - depending on the noop_mask
     * |  |1    |   |  |1    |   |    1|  |   |    1|  |
     * |  |  2  |   |  `-----+   +-----'  |   |  2  |  |
     * |  |    3|   |↗       |   |       ↖|   |3    |  |
     * `--+-----'   `--------'   `--------'   `-----+--'
     * round:   1            2            3            4
     * Run in this order: 0-2-3-4-1
     * zero means we wipe out lightmap.
     * But why start from 2? No special reason.
     */



    return;
}
