## actually blending
1. On Krita, load the cloudmap
2. Select and isolate suitable piece of the cloud
3. With liquefying brush, make the bottom flat
4. On new tab, load the solidmap
5. Resize the solidmap to 1024x1024
6. Rotate the solidmap by 30 deg or so
7. Overlay the processed solidmap to the cloudmap, opacity=30%
8. With airbrush tool, manually add the shade (see cloud_normal.kra to get the gist of it)

### cloudmap
1. On GIMP, prepare 4096x4096 canvas
2. Create Simplex Noise with following parameters:
    - Scale: arbitrary (0.05 - 0.2)
    - Iterations: MAX
    - Seed: anything but 0

### solidmap
1. On GIMP, prepare 4096x4096 canvas
2. Create Solid Noise with following parameters:
    - XY Size: 16.0
    - Detail: MAX
    - Tileable: check
    - Turbulent: check
    - Seed: anything but 0

