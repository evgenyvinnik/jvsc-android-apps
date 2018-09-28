/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.qualcomm.qti.polyqon;

import java.nio.Buffer;



class Plane extends MeshObject
{
    // Data for drawing the 3D plane as overlay
    private static final float planeVertices[] = { -0.5f, -0.5f, 0.0f, 0.5f,
            -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -0.5f, 0.5f, 0.0f };
    
    private static final float planeTexcoords[] = { 0.0f, 0.0f, 0.75f, 0.0f,
            0.75f, 0.75f, 0.0f, 0.75f };
    
    private static final float planeNormals[] = { 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f };
    
    private static final short planeIndices[] = { 0, 1, 2, 0, 2, 3 };
    
    Buffer verts;
    Buffer textCoords;
    Buffer norms;
    Buffer indices;
    
    
    public Plane()
    {
        verts = fillBuffer(planeVertices);
        textCoords = fillBuffer(planeTexcoords);
        norms = fillBuffer(planeNormals);
        indices = fillBuffer(planeIndices);
    }
    
    
    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
    {
        Buffer result = null;
        switch (bufferType)
        {
            case BUFFER_TYPE_VERTEX:
                result = verts;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = textCoords;
                break;
            case BUFFER_TYPE_INDICES:
                result = indices;
                break;
            case BUFFER_TYPE_NORMALS:
                result = norms;
            default:
                break;
        }
        return result;
    }
    
    
    @Override
    public int getNumObjectVertex()
    {
        return planeVertices.length / 3;
    }
    
    
    @Override
    public int getNumObjectIndex()
    {
        return planeIndices.length;
    }
}
