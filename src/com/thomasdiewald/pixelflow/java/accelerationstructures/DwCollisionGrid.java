/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.accelerationstructures;


public class DwCollisionGrid{
  
  private float CELL_SIZE = 10f;
  private int   GRID_X; 
//  private int   GRID_Y;

  private int               HEAD_PTR;
  private int[]             HEAD = new int[0];
  private int[]             NEXT = new int[0];
  private DwCollisionObject[] DATA = new DwCollisionObject[0];
  
//  private float[] cache_xyr = new float[0];
  
  public DwCollisionGrid(){
  }
  
  private void resize(int gx, int gy, int PPLL_size){
    
    // HEAD pointers
//    if( gx > GRID_X || gy > GRID_Y){
    if( (gx * gy) > HEAD.length){
      HEAD = new int[gx * gy];
//      System.out.println("CollisionGridAccelerator.resize -> HEAD: "+gx+", "+gy);
    }

    // NEXT pointers, DATA array
    if(PPLL_size > NEXT.length){
      int size_new = (int)(PPLL_size * 1.2f);
      NEXT = new int            [size_new];
      DATA = new DwCollisionObject[size_new];
//      System.out.println("CollisionGridAccelerator.resize -> NEXT/DATA: "+size_new+", "+PPLL_size);
    }
    
    // clear NEXT pointers
    for(int i = 0; i < HEAD.length; i++) HEAD[i] = 0;
//    for(int i = 0; i < NEXT.length; i++) NEXT[i] = 0;
//    for(int i = 0; i < DATA.length; i++) DATA[i] = null; 

    // reset HEAD pointer
    HEAD_PTR = 0;
    
    // set grid size
    GRID_X = gx;
//    GRID_Y = gy;
  }
  
  
  
  private void create(DwCollisionObject[] particles, int num_particles){

    for(int i = 0; i < num_particles; i++){
      DwCollisionObject particle = particles[i];
      float pr = particle.radCollision();
      float px = particle.x();
      float py = particle.y();
      
      px -= bounds[0];
      py -= bounds[1];
      
      int xmin = (int)((px-pr)/CELL_SIZE); // xmin = Math.max(xmin, 0);
      int xmax = (int)((px+pr)/CELL_SIZE); // xmax = Math.min(xmax, GRID_X-1);
      int ymin = (int)((py-pr)/CELL_SIZE); // ymin = Math.max(ymin, 0);
      int ymax = (int)((py+pr)/CELL_SIZE); // ymax = Math.min(ymax, GRID_Y-1);
      
      for(int y = ymin; y <= ymax ; y++){
        for(int x = xmin; x <= xmax ; x++){
          int gid = y * GRID_X + x;
          int new_head = HEAD_PTR++;
          int old_head = HEAD[gid]; HEAD[gid] = new_head; // xchange head pointer
          if(new_head < NEXT.length){
            NEXT[new_head] = old_head;
            DATA[new_head] = particle;
          } else {
            // keep counting for reallocation size
          }
        }
      }
    }
    
  }
  
  
  
  private void solveCollisions(DwCollisionObject[] particles, int num_particles){
    
    // reset states
    for(int i = 0; i < num_particles; i++){
      particles[i].resetCollisionPtr();
    }
    
    // solve collisions
    for(int i = 0; i < num_particles; i++){
      DwCollisionObject particle = particles[i];

      float pr = particle.radCollision();
      float px = particle.x();
      float py = particle.y();
      
//      float px = cache_xyr[i*3+0];
//      float py = cache_xyr[i*3+1];
//      float pr = cache_xyr[i*3+2];
      
      px -= bounds[0];
      py -= bounds[1];
      
      int xmin = (int)((px-pr)/CELL_SIZE); // xmin = Math.max(xmin, 0);
      int xmax = (int)((px+pr)/CELL_SIZE); // xmax = Math.min(xmax, GRID_X-1);
      int ymin = (int)((py-pr)/CELL_SIZE); // ymin = Math.max(ymin, 0);
      int ymax = (int)((py+pr)/CELL_SIZE); // ymax = Math.min(ymax, GRID_Y-1);

      for(int y = ymin; y <= ymax ; y++){
        for(int x = xmin; x <= xmax ; x++){
          int gid = y * GRID_X + x;
          int head = HEAD[gid];
          while(head > 0){
            DwCollisionObject othr = DATA[head];
            particle.update(othr);  
            head = NEXT[head];
          }
        }
      }
        
    }
  }
  
  
  public float[] bounds = new float[4];
  
  public void computeBounds(DwCollisionObject[] particles, int num_particles){ 
    float x_min = +Float.MAX_VALUE;
    float y_min = +Float.MAX_VALUE;
    float x_max = -Float.MAX_VALUE;
    float y_max = -Float.MAX_VALUE;
    
    CELL_SIZE = 1;
    
    float r_sum = 0;
    
    for(int i = 0; i < num_particles; i++){
      float x = particles[i].x();
      float y = particles[i].y();
      float r = particles[i].radCollision();
      r_sum += r;
      
      if(x-r < x_min) x_min = x-r;
      if(x+r > x_max) x_max = x+r;
      if(y-r < y_min) y_min = y-r;
      if(y+r > y_max) y_max = y+r;
      
      // use max radius
//      if(r*2 > CELL_SIZE) CELL_SIZE = r*2; 
    }
    
    bounds[0] = x_min;
    bounds[1] = y_min;
    bounds[2] = x_max;
    bounds[3] = y_max;
    
    CELL_SIZE = (r_sum * 2) /particles.length;
  }
  
  
//  public void cacheXYR(CollisionObject[] particles, int num_particles){
//    
//    int size_min = num_particles*3;
//    if(cache_xyr.length < size_min){
//      cache_xyr = new float[size_min];
//    }
//    for(int i = 0, idx = 0; i < num_particles; i++){
//      CollisionObject particle = particles[i];
//      cache_xyr[idx++] = particle.x();
//      cache_xyr[idx++] = particle.y();
//      cache_xyr[idx++] = particle.rad();
//    }
//  }
  
  
  public void updateCollisions(DwCollisionObject[] particles){
    updateCollisions(particles, particles.length);
  }
  
  public void updateCollisions(DwCollisionObject[] particles, int num_particles){

    // 0) prepare dimensions, size,
//    cacheXYR(particles, num_particles);
    computeBounds(particles, num_particles);
    int gx = (int) Math.ceil((bounds[2] - bounds[0])/CELL_SIZE)+1;
    int gy = (int) Math.ceil((bounds[3] - bounds[1])/CELL_SIZE)+1;
    int ppll_len = particles.length * 4 + 1; // just an estimate
    
    // 1) resize if necessary
    resize(gx, gy, ppll_len);
    
    // 2) create per-pixel-linked-list (PPLL)
    create(particles, num_particles);
    
    // resize if necessary
    if(HEAD_PTR > NEXT.length){
      resize(gx, gy, HEAD_PTR);
      create(particles, num_particles);
    }
    
    // 3) solve collisions for each particle
    solveCollisions(particles, num_particles);
  }

  
}