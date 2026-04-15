package com.goodchip.ledstrip;



public class LedStripJni {
	private static LedStripJni ledstrip=null;
	private final int ROTATE_ANGEL=270;
	synchronized public static LedStripJni getLedstrip(){
		if(ledstrip==null)
			return new LedStripJni();
		else
			return ledstrip;
	}

	private LedStripJni(){}
    public native static int Init();
	public native static int sendData(int[] data);
	public native static int DeInit();

    static {
    	System.loadLibrary("ledstrip");
    }

}
