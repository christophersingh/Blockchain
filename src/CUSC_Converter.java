import java.math.*;
public class CUSC_Converter {          

	
	public static float convertToUSD(float CUSC){
		return (float) (CUSC * 0.12);
	}
	
	//Let's use the ceiling function to round up when converting back to CUSC coins
	public static float convertToCUSC(float USD){
		return (float) (Math.ceil((USD * 8.33333)));
	}

}

