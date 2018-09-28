package ca.jvsh.musagi;

public class FFT
{
	/*

	--- FFT code by Takuya OOURA (email: ooura@mmm.t.u-tokyo.ac.jp) ---


	Fast Fourier/Cosine/Sine Transform
	    dimension   :one
	    data length :power of 2
	    decimation  :frequency
	    radix       :split-radix
	    data        :inplace
	    table       :use
	functions
	    cdft: Complex Discrete Fourier Transform
	    rdft: Real Discrete Fourier Transform
	    ddct: Discrete Cosine Transform
	    ddst: Discrete Sine Transform
	    dfct: Cosine Transform of RDFT (Real Symmetric DFT)
	    dfst: Sine Transform of RDFT (Real Anti-symmetric DFT)
	function prototypes
	    void cdft(int, int, double *, int *, double *);
	    void rdft(int, int, double *, int *, double *);
	    void ddct(int, int, double *, int *, double *);
	    void ddst(int, int, double *, int *, double *);
	    void dfct(int, double *, double *, int *, double *);
	    void dfst(int, double *, double *, int *, double *);
	macro definitions
	    USE_CDFT_PTHREADS : default=not defined
	        CDFT_THREADS_BEGIN_N  : must be >= 512, default=8192
	        CDFT_4THREADS_BEGIN_N : must be >= 512, default=65536
	    USE_CDFT_WINTHREADS : default=not defined
	        CDFT_THREADS_BEGIN_N  : must be >= 512, default=32768
	        CDFT_4THREADS_BEGIN_N : must be >= 512, default=524288


	-------- Complex DFT (Discrete Fourier Transform) --------
	    [definition]
	        <case1>
	            X[k] = sum_j=0^n-1 x[j]*exp(2*pi*i*j*k/n), 0<=k<n
	        <case2>
	            X[k] = sum_j=0^n-1 x[j]*exp(-2*pi*i*j*k/n), 0<=k<n
	        (notes: sum_j=0^n-1 is a summation from j=0 to n-1)
	    [usage]
	        <case1>
	            ip[0] = 0; // first time only
	            cdft(2*n, 1, a, ip, w);
	        <case2>
	            ip[0] = 0; // first time only
	            cdft(2*n, -1, a, ip, w);
	    [parameters]
	        2*n            :data length (int)
	                        n >= 1, n = power of 2
	        a[0...2*n-1]   :input/output data (double *)
	                        input data
	                            a[2*j] = Re(x[j]), 
	                            a[2*j+1] = Im(x[j]), 0<=j<n
	                        output data
	                            a[2*k] = Re(X[k]), 
	                            a[2*k+1] = Im(X[k]), 0<=k<n
	        ip[0...*]      :work area for bit reversal (int *)
	                        length of ip >= 2+sqrt(n)
	                        strictly, 
	                        length of ip >= 
	                            2+(1<<(int)(log(n+0.5)/log(2))/2).
	                        ip[0],ip[1] are pointers of the cos/sin table.
	        w[0...n/2-1]   :cos/sin table (double *)
	                        w[],ip[] are initialized if ip[0] == 0.
	    [remark]
	        Inverse of 
	            cdft(2*n, -1, a, ip, w);
	        is 
	            cdft(2*n, 1, a, ip, w);
	            for (j = 0; j <= 2 * n - 1; j++) {
	                a[j] *= 1.0 / n;
	            }
	        .


	Appendix :
	    The cos/sin table is recalculated when the larger table required.
	    w[] and ip[] are compatible with all routines.
	*/


	public static void cdft(int n, int isgn, double[] a, int[] ip, double[] w)
	{
	    int nw;
	    
	    nw = ip[0];
	    if (n > (nw << 2)) {
	        nw = n >> 2;
	        makewt(nw, ip, w);
	    }
	    if (isgn >= 0) {
	        cftfsub(n, a, ip, nw, w);
	    } else {
	        cftbsub(n, a, ip, nw, w);
	    }
	}

	public static void makewt(int nw, int[] ip, double[] w)
	{
	    int j, nwh, nw0, nw1;
	    double delta, wn4r, wk1r, wk1i, wk3r, wk3i;
	    
	    ip[0] = nw;
	    ip[1] = 1;
	    if (nw > 2) {
	        nwh = nw >> 1;
	        delta = Math.atan(1.0) / nwh;
	        wn4r = Math.cos(delta * nwh);
	        w[0] = 1;
	        w[1] = wn4r;
	        if (nwh == 4) {
	            w[2] = Math.cos(delta * 2);
	            w[3] = Math.sin(delta * 2);
	        } else if (nwh > 4) {
	            makeipt(nw, ip);
	            w[2] = 0.5 / Math.cos(delta * 2);
	            w[3] = 0.5 / Math.cos(delta * 6);
	            for (j = 4; j < nwh; j += 4) {
	                w[j] = Math.cos(delta * j);
	                w[j + 1] = Math.sin(delta * j);
	                w[j + 2] = Math.cos(3 * delta * j);
	                w[j + 3] = -Math.sin(3 * delta * j);
	            }
	        }
	        nw0 = 0;
	        while (nwh > 2) {
	            nw1 = nw0 + nwh;
	            nwh >>= 1;
	            w[nw1] = 1;
	            w[nw1 + 1] = wn4r;
	            if (nwh == 4) {
	                wk1r = w[nw0 + 4];
	                wk1i = w[nw0 + 5];
	                w[nw1 + 2] = wk1r;
	                w[nw1 + 3] = wk1i;
	            } else if (nwh > 4) {
	                wk1r = w[nw0 + 4];
	                wk3r = w[nw0 + 6];
	                w[nw1 + 2] = 0.5 / wk1r;
	                w[nw1 + 3] = 0.5 / wk3r;
	                for (j = 4; j < nwh; j += 4) {
	                    wk1r = w[nw0 + 2 * j];
	                    wk1i = w[nw0 + 2 * j + 1];
	                    wk3r = w[nw0 + 2 * j + 2];
	                    wk3i = w[nw0 + 2 * j + 3];
	                    w[nw1 + j] = wk1r;
	                    w[nw1 + j + 1] = wk1i;
	                    w[nw1 + j + 2] = wk3r;
	                    w[nw1 + j + 3] = wk3i;
	                }
	            }
	            nw0 = nw1;
	        }
	    }
	}

	public static void makeipt(int nw, int [] ip)
	{
	    int j, l, m, m2, p, q;
	    
	    ip[2] = 0;
	    ip[3] = 16;
	    m = 2;
	    for (l = nw; l > 32; l >>= 2) {
	        m2 = m << 1;
	        q = m2 << 3;
	        for (j = m; j < m2; j++) {
	            p = ip[j] << 2;
	            ip[m + j] = p;
	            ip[m2 + j] = p + q;
	        }
	        m = m2;
	    }
	}


	public static void makect(int nc, int []ip, double [] c)
	{
	    int j, nch;
	    double delta;
	    
	    ip[1] = nc;
	    if (nc > 1) {
	        nch = nc >> 1;
	        delta = Math.atan(1.0) / nch;
	        c[0] = Math.cos(delta * nch);
	        c[nch] = 0.5 * c[0];
	        for (j = 1; j < nch; j++) {
	            c[j] = 0.5 * Math.cos(delta * j);
	            c[nc - j] = 0.5 * Math.sin(delta * j);
	        }
	    }
	}

	public static void cftfsub(int n, double []a, int []ip, int nw, double []w)
	{
	    if (n > 8) {
	        if (n > 32) {
	            cftf1st(n, a, w, nw - (n >> 2));

	            if (n > 512) {
	                cftrec4(n, a, nw, w);
	            } else if (n > 128) {
	                cftleaf(n, 1, a, 0, nw, w);
	            } else {
	                cftfx41(n, a, nw, w);
	            }
	            bitrv2(n, ip, a);
	        } else if (n == 32) {
	            cftf161(a, 0, w, nw - 8);
	            bitrv216(a);
	        } else {
	            cftf081(a, 0, w, 0);
	            bitrv208(a);
	        }
	    } else if (n == 8) {
	        cftf040(a);
	    } else if (n == 4) {
	        cftx020(a);
	    }
	}


	public static void cftbsub(int n, double []a, int []ip, int nw, double []w)
	{
	    
	    if (n > 8) {
	        if (n > 32) {
	            cftb1st(n, a, w, nw - (n >> 2));

	            if (n > 512) {
	                cftrec4(n, a, nw, w);
	            } else if (n > 128) {
	                cftleaf(n, 1, a, 0, nw, w);
	            } else {
	                cftfx41(n, a, nw, w);
	            }
	            bitrv2conj(n, ip, a);
	        } else if (n == 32) {
	            cftf161(a, 0, w, nw - 8);
	            bitrv216neg(a);
	        } else {
	            cftf081(a, 0, w, 0);
	            bitrv208neg(a);
	        }
	    } else if (n == 8) {
	        cftb040(a);
	    } else if (n == 4) {
	        cftx020(a);
	    }
	}


	public static void bitrv2(int n, int []ip, double []a)
	{
	    int j, j1, k, k1, l, m, nh, nm;
	    double xr, xi, yr, yi;
	    
	    m = 1;
	    for (l = n >> 2; l > 8; l >>= 2) {
	        m <<= 1;
	    }
	    nh = n >> 1;
	    nm = 4 * m;
	    if (l == 8) {
	        for (k = 0; k < m; k++) {
	            for (j = 0; j < k; j++) {
	                j1 = 4 * j + 2 * ip[m + k];
	                k1 = 4 * k + 2 * ip[m + j];
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nh;
	                k1 += 2;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += 2;
	                k1 += nh;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nh;
	                k1 -= 2;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	            }
	            k1 = 4 * k + 2 * ip[m + k];
	            j1 = k1 + 2;
	            k1 += nh;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 += nm;
	            k1 += 2 * nm;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 += nm;
	            k1 -= nm;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 -= 2;
	            k1 -= nh;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 += nh + 2;
	            k1 += nh + 2;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 -= nh - nm;
	            k1 += 2 * nm - 2;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	        }
	    } else {
	        for (k = 0; k < m; k++) {
	            for (j = 0; j < k; j++) {
	                j1 = 4 * j + ip[m + k];
	                k1 = 4 * k + ip[m + j];
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nh;
	                k1 += 2;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += 2;
	                k1 += nh;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nh;
	                k1 -= 2;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = a[j1 + 1];
	                yr = a[k1];
	                yi = a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	            }
	            k1 = 4 * k + ip[m + k];
	            j1 = k1 + 2;
	            k1 += nh;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 += nm;
	            k1 += nm;
	            xr = a[j1];
	            xi = a[j1 + 1];
	            yr = a[k1];
	            yi = a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	        }
	    }
	}


	public static void bitrv2conj(int n, int []ip, double []a)
	{
	    int j, j1, k, k1, l, m, nh, nm;
	    double xr, xi, yr, yi;
	    
	    m = 1;
	    for (l = n >> 2; l > 8; l >>= 2) {
	        m <<= 1;
	    }
	    nh = n >> 1;
	    nm = 4 * m;
	    if (l == 8) {
	        for (k = 0; k < m; k++) {
	            for (j = 0; j < k; j++) {
	                j1 = 4 * j + 2 * ip[m + k];
	                k1 = 4 * k + 2 * ip[m + j];
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nh;
	                k1 += 2;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += 2;
	                k1 += nh;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nh;
	                k1 -= 2;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= 2 * nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	            }
	            k1 = 4 * k + 2 * ip[m + k];
	            j1 = k1 + 2;
	            k1 += nh;
	            a[j1 - 1] = -a[j1 - 1];
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            a[k1 + 3] = -a[k1 + 3];
	            j1 += nm;
	            k1 += 2 * nm;
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 += nm;
	            k1 -= nm;
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 -= 2;
	            k1 -= nh;
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 += nh + 2;
	            k1 += nh + 2;
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            j1 -= nh - nm;
	            k1 += 2 * nm - 2;
	            a[j1 - 1] = -a[j1 - 1];
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            a[k1 + 3] = -a[k1 + 3];
	        }
	    } else {
	        for (k = 0; k < m; k++) {
	            for (j = 0; j < k; j++) {
	                j1 = 4 * j + ip[m + k];
	                k1 = 4 * k + ip[m + j];
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nh;
	                k1 += 2;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += 2;
	                k1 += nh;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 += nm;
	                k1 += nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nh;
	                k1 -= 2;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	                j1 -= nm;
	                k1 -= nm;
	                xr = a[j1];
	                xi = -a[j1 + 1];
	                yr = a[k1];
	                yi = -a[k1 + 1];
	                a[j1] = yr;
	                a[j1 + 1] = yi;
	                a[k1] = xr;
	                a[k1 + 1] = xi;
	            }
	            k1 = 4 * k + ip[m + k];
	            j1 = k1 + 2;
	            k1 += nh;
	            a[j1 - 1] = -a[j1 - 1];
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            a[k1 + 3] = -a[k1 + 3];
	            j1 += nm;
	            k1 += nm;
	            a[j1 - 1] = -a[j1 - 1];
	            xr = a[j1];
	            xi = -a[j1 + 1];
	            yr = a[k1];
	            yi = -a[k1 + 1];
	            a[j1] = yr;
	            a[j1 + 1] = yi;
	            a[k1] = xr;
	            a[k1 + 1] = xi;
	            a[k1 + 3] = -a[k1 + 3];
	        }
	    }
	}


	public static void bitrv216(double []a)
	{
	    double x1r, x1i, x2r, x2i, x3r, x3i, x4r, x4i, 
	        x5r, x5i, x7r, x7i, x8r, x8i, x10r, x10i, 
	        x11r, x11i, x12r, x12i, x13r, x13i, x14r, x14i;
	    
	    x1r = a[2];
	    x1i = a[3];
	    x2r = a[4];
	    x2i = a[5];
	    x3r = a[6];
	    x3i = a[7];
	    x4r = a[8];
	    x4i = a[9];
	    x5r = a[10];
	    x5i = a[11];
	    x7r = a[14];
	    x7i = a[15];
	    x8r = a[16];
	    x8i = a[17];
	    x10r = a[20];
	    x10i = a[21];
	    x11r = a[22];
	    x11i = a[23];
	    x12r = a[24];
	    x12i = a[25];
	    x13r = a[26];
	    x13i = a[27];
	    x14r = a[28];
	    x14i = a[29];
	    a[2] = x8r;
	    a[3] = x8i;
	    a[4] = x4r;
	    a[5] = x4i;
	    a[6] = x12r;
	    a[7] = x12i;
	    a[8] = x2r;
	    a[9] = x2i;
	    a[10] = x10r;
	    a[11] = x10i;
	    a[14] = x14r;
	    a[15] = x14i;
	    a[16] = x1r;
	    a[17] = x1i;
	    a[20] = x5r;
	    a[21] = x5i;
	    a[22] = x13r;
	    a[23] = x13i;
	    a[24] = x3r;
	    a[25] = x3i;
	    a[26] = x11r;
	    a[27] = x11i;
	    a[28] = x7r;
	    a[29] = x7i;
	}


	public static void bitrv216neg(double []a)
	{
	    double x1r, x1i, x2r, x2i, x3r, x3i, x4r, x4i, 
	        x5r, x5i, x6r, x6i, x7r, x7i, x8r, x8i, 
	        x9r, x9i, x10r, x10i, x11r, x11i, x12r, x12i, 
	        x13r, x13i, x14r, x14i, x15r, x15i;
	    
	    x1r = a[2];
	    x1i = a[3];
	    x2r = a[4];
	    x2i = a[5];
	    x3r = a[6];
	    x3i = a[7];
	    x4r = a[8];
	    x4i = a[9];
	    x5r = a[10];
	    x5i = a[11];
	    x6r = a[12];
	    x6i = a[13];
	    x7r = a[14];
	    x7i = a[15];
	    x8r = a[16];
	    x8i = a[17];
	    x9r = a[18];
	    x9i = a[19];
	    x10r = a[20];
	    x10i = a[21];
	    x11r = a[22];
	    x11i = a[23];
	    x12r = a[24];
	    x12i = a[25];
	    x13r = a[26];
	    x13i = a[27];
	    x14r = a[28];
	    x14i = a[29];
	    x15r = a[30];
	    x15i = a[31];
	    a[2] = x15r;
	    a[3] = x15i;
	    a[4] = x7r;
	    a[5] = x7i;
	    a[6] = x11r;
	    a[7] = x11i;
	    a[8] = x3r;
	    a[9] = x3i;
	    a[10] = x13r;
	    a[11] = x13i;
	    a[12] = x5r;
	    a[13] = x5i;
	    a[14] = x9r;
	    a[15] = x9i;
	    a[16] = x1r;
	    a[17] = x1i;
	    a[18] = x14r;
	    a[19] = x14i;
	    a[20] = x6r;
	    a[21] = x6i;
	    a[22] = x10r;
	    a[23] = x10i;
	    a[24] = x2r;
	    a[25] = x2i;
	    a[26] = x12r;
	    a[27] = x12i;
	    a[28] = x4r;
	    a[29] = x4i;
	    a[30] = x8r;
	    a[31] = x8i;
	}


	public static void bitrv208(double[]a)
	{
	    double x1r, x1i, x3r, x3i, x4r, x4i, x6r, x6i;
	    
	    x1r = a[2];
	    x1i = a[3];
	    x3r = a[6];
	    x3i = a[7];
	    x4r = a[8];
	    x4i = a[9];
	    x6r = a[12];
	    x6i = a[13];
	    a[2] = x4r;
	    a[3] = x4i;
	    a[6] = x6r;
	    a[7] = x6i;
	    a[8] = x1r;
	    a[9] = x1i;
	    a[12] = x3r;
	    a[13] = x3i;
	}


	public static void bitrv208neg(double []a)
	{
	    double x1r, x1i, x2r, x2i, x3r, x3i, x4r, x4i, 
	        x5r, x5i, x6r, x6i, x7r, x7i;
	    
	    x1r = a[2];
	    x1i = a[3];
	    x2r = a[4];
	    x2i = a[5];
	    x3r = a[6];
	    x3i = a[7];
	    x4r = a[8];
	    x4i = a[9];
	    x5r = a[10];
	    x5i = a[11];
	    x6r = a[12];
	    x6i = a[13];
	    x7r = a[14];
	    x7i = a[15];
	    a[2] = x7r;
	    a[3] = x7i;
	    a[4] = x3r;
	    a[5] = x3i;
	    a[6] = x5r;
	    a[7] = x5i;
	    a[8] = x1r;
	    a[9] = x1i;
	    a[10] = x6r;
	    a[11] = x6i;
	    a[12] = x2r;
	    a[13] = x2i;
	    a[14] = x4r;
	    a[15] = x4i;
	}


	public static void cftf1st(int n, double []a, double []w, int w_start)
	{
	    int j, j0, j1, j2, j3, k, m, mh;
	    double wn4r, csc1, csc3, wk1r, wk1i, wk3r, wk3i, 
	        wd1r, wd1i, wd3r, wd3i;
	    double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, 
	        y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i;
	    
	    mh = n >> 3;
	    m = 2 * mh;
	    j1 = m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[0] + a[j2];
	    x0i = a[1] + a[j2 + 1];
	    x1r = a[0] - a[j2];
	    x1i = a[1] - a[j2 + 1];
	    x2r = a[j1] + a[j3];
	    x2i = a[j1 + 1] + a[j3 + 1];
	    x3r = a[j1] - a[j3];
	    x3i = a[j1 + 1] - a[j3 + 1];
	    a[0] = x0r + x2r;
	    a[1] = x0i + x2i;
	    a[j1] = x0r - x2r;
	    a[j1 + 1] = x0i - x2i;
	    a[j2] = x1r - x3i;
	    a[j2 + 1] = x1i + x3r;
	    a[j3] = x1r + x3i;
	    a[j3 + 1] = x1i - x3r;
	    wn4r = w[w_start + 1];
	    csc1 = w[w_start + 2];
	    csc3 = w[w_start + 3];
	    wd1r = 1;
	    wd1i = 0;
	    wd3r = 1;
	    wd3i = 0;
	    k = 0;
	    for (j = 2; j < mh - 2; j += 4) {
	        k += 4;
	        wk1r = csc1 * (wd1r + w[w_start + k]);
	        wk1i = csc1 * (wd1i + w[w_start + k + 1]);
	        wk3r = csc3 * (wd3r + w[w_start + k + 2]);
	        wk3i = csc3 * (wd3i + w[w_start + k + 3]);
	        wd1r = w[w_start + k];
	        wd1i = w[w_start + k + 1];
	        wd3r = w[w_start + k + 2];
	        wd3i = w[w_start + k + 3];
	        j1 = j + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[j] + a[j2];
	        x0i = a[j + 1] + a[j2 + 1];
	        x1r = a[j] - a[j2];
	        x1i = a[j + 1] - a[j2 + 1];
	        y0r = a[j + 2] + a[j2 + 2];
	        y0i = a[j + 3] + a[j2 + 3];
	        y1r = a[j + 2] - a[j2 + 2];
	        y1i = a[j + 3] - a[j2 + 3];
	        x2r = a[j1] + a[j3];
	        x2i = a[j1 + 1] + a[j3 + 1];
	        x3r = a[j1] - a[j3];
	        x3i = a[j1 + 1] - a[j3 + 1];
	        y2r = a[j1 + 2] + a[j3 + 2];
	        y2i = a[j1 + 3] + a[j3 + 3];
	        y3r = a[j1 + 2] - a[j3 + 2];
	        y3i = a[j1 + 3] - a[j3 + 3];
	        a[j] = x0r + x2r;
	        a[j + 1] = x0i + x2i;
	        a[j + 2] = y0r + y2r;
	        a[j + 3] = y0i + y2i;
	        a[j1] = x0r - x2r;
	        a[j1 + 1] = x0i - x2i;
	        a[j1 + 2] = y0r - y2r;
	        a[j1 + 3] = y0i - y2i;
	        x0r = x1r - x3i;
	        x0i = x1i + x3r;
	        a[j2] = wk1r * x0r - wk1i * x0i;
	        a[j2 + 1] = wk1r * x0i + wk1i * x0r;
	        x0r = y1r - y3i;
	        x0i = y1i + y3r;
	        a[j2 + 2] = wd1r * x0r - wd1i * x0i;
	        a[j2 + 3] = wd1r * x0i + wd1i * x0r;
	        x0r = x1r + x3i;
	        x0i = x1i - x3r;
	        a[j3] = wk3r * x0r + wk3i * x0i;
	        a[j3 + 1] = wk3r * x0i - wk3i * x0r;
	        x0r = y1r + y3i;
	        x0i = y1i - y3r;
	        a[j3 + 2] = wd3r * x0r + wd3i * x0i;
	        a[j3 + 3] = wd3r * x0i - wd3i * x0r;
	        j0 = m - j;
	        j1 = j0 + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[j0] + a[j2];
	        x0i = a[j0 + 1] + a[j2 + 1];
	        x1r = a[j0] - a[j2];
	        x1i = a[j0 + 1] - a[j2 + 1];
	        y0r = a[j0 - 2] + a[j2 - 2];
	        y0i = a[j0 - 1] + a[j2 - 1];
	        y1r = a[j0 - 2] - a[j2 - 2];
	        y1i = a[j0 - 1] - a[j2 - 1];
	        x2r = a[j1] + a[j3];
	        x2i = a[j1 + 1] + a[j3 + 1];
	        x3r = a[j1] - a[j3];
	        x3i = a[j1 + 1] - a[j3 + 1];
	        y2r = a[j1 - 2] + a[j3 - 2];
	        y2i = a[j1 - 1] + a[j3 - 1];
	        y3r = a[j1 - 2] - a[j3 - 2];
	        y3i = a[j1 - 1] - a[j3 - 1];
	        a[j0] = x0r + x2r;
	        a[j0 + 1] = x0i + x2i;
	        a[j0 - 2] = y0r + y2r;
	        a[j0 - 1] = y0i + y2i;
	        a[j1] = x0r - x2r;
	        a[j1 + 1] = x0i - x2i;
	        a[j1 - 2] = y0r - y2r;
	        a[j1 - 1] = y0i - y2i;
	        x0r = x1r - x3i;
	        x0i = x1i + x3r;
	        a[j2] = wk1i * x0r - wk1r * x0i;
	        a[j2 + 1] = wk1i * x0i + wk1r * x0r;
	        x0r = y1r - y3i;
	        x0i = y1i + y3r;
	        a[j2 - 2] = wd1i * x0r - wd1r * x0i;
	        a[j2 - 1] = wd1i * x0i + wd1r * x0r;
	        x0r = x1r + x3i;
	        x0i = x1i - x3r;
	        a[j3] = wk3i * x0r + wk3r * x0i;
	        a[j3 + 1] = wk3i * x0i - wk3r * x0r;
	        x0r = y1r + y3i;
	        x0i = y1i - y3r;
	        a[j3 - 2] = wd3i * x0r + wd3r * x0i;
	        a[j3 - 1] = wd3i * x0i - wd3r * x0r;
	    }
	    wk1r = csc1 * (wd1r + wn4r);
	    wk1i = csc1 * (wd1i + wn4r);
	    wk3r = csc3 * (wd3r - wn4r);
	    wk3i = csc3 * (wd3i - wn4r);
	    j0 = mh;
	    j1 = j0 + m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[j0 - 2] + a[j2 - 2];
	    x0i = a[j0 - 1] + a[j2 - 1];
	    x1r = a[j0 - 2] - a[j2 - 2];
	    x1i = a[j0 - 1] - a[j2 - 1];
	    x2r = a[j1 - 2] + a[j3 - 2];
	    x2i = a[j1 - 1] + a[j3 - 1];
	    x3r = a[j1 - 2] - a[j3 - 2];
	    x3i = a[j1 - 1] - a[j3 - 1];
	    a[j0 - 2] = x0r + x2r;
	    a[j0 - 1] = x0i + x2i;
	    a[j1 - 2] = x0r - x2r;
	    a[j1 - 1] = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    a[j2 - 2] = wk1r * x0r - wk1i * x0i;
	    a[j2 - 1] = wk1r * x0i + wk1i * x0r;
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    a[j3 - 2] = wk3r * x0r + wk3i * x0i;
	    a[j3 - 1] = wk3r * x0i - wk3i * x0r;
	    x0r = a[j0] + a[j2];
	    x0i = a[j0 + 1] + a[j2 + 1];
	    x1r = a[j0] - a[j2];
	    x1i = a[j0 + 1] - a[j2 + 1];
	    x2r = a[j1] + a[j3];
	    x2i = a[j1 + 1] + a[j3 + 1];
	    x3r = a[j1] - a[j3];
	    x3i = a[j1 + 1] - a[j3 + 1];
	    a[j0] = x0r + x2r;
	    a[j0 + 1] = x0i + x2i;
	    a[j1] = x0r - x2r;
	    a[j1 + 1] = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    a[j2] = wn4r * (x0r - x0i);
	    a[j2 + 1] = wn4r * (x0i + x0r);
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    a[j3] = -wn4r * (x0r + x0i);
	    a[j3 + 1] = -wn4r * (x0i - x0r);
	    x0r = a[j0 + 2] + a[j2 + 2];
	    x0i = a[j0 + 3] + a[j2 + 3];
	    x1r = a[j0 + 2] - a[j2 + 2];
	    x1i = a[j0 + 3] - a[j2 + 3];
	    x2r = a[j1 + 2] + a[j3 + 2];
	    x2i = a[j1 + 3] + a[j3 + 3];
	    x3r = a[j1 + 2] - a[j3 + 2];
	    x3i = a[j1 + 3] - a[j3 + 3];
	    a[j0 + 2] = x0r + x2r;
	    a[j0 + 3] = x0i + x2i;
	    a[j1 + 2] = x0r - x2r;
	    a[j1 + 3] = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    a[j2 + 2] = wk1i * x0r - wk1r * x0i;
	    a[j2 + 3] = wk1i * x0i + wk1r * x0r;
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    a[j3 + 2] = wk3i * x0r + wk3r * x0i;
	    a[j3 + 3] = wk3i * x0i - wk3r * x0r;
	}


	public static void cftb1st(int n, double []a, double []w, int w_start)
	{
	    int j, j0, j1, j2, j3, k, m, mh;
	    double wn4r, csc1, csc3, wk1r, wk1i, wk3r, wk3i, 
	        wd1r, wd1i, wd3r, wd3i;
	    double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, 
	        y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i;
	    
	    mh = n >> 3;
	    m = 2 * mh;
	    j1 = m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[0] + a[j2];
	    x0i = -a[1] - a[j2 + 1];
	    x1r = a[0] - a[j2];
	    x1i = -a[1] + a[j2 + 1];
	    x2r = a[j1] + a[j3];
	    x2i = a[j1 + 1] + a[j3 + 1];
	    x3r = a[j1] - a[j3];
	    x3i = a[j1 + 1] - a[j3 + 1];
	    a[0] = x0r + x2r;
	    a[1] = x0i - x2i;
	    a[j1] = x0r - x2r;
	    a[j1 + 1] = x0i + x2i;
	    a[j2] = x1r + x3i;
	    a[j2 + 1] = x1i + x3r;
	    a[j3] = x1r - x3i;
	    a[j3 + 1] = x1i - x3r;
	    wn4r = w[w_start + 1];
	    csc1 = w[w_start + 2];
	    csc3 = w[w_start + 3];
	    wd1r = 1;
	    wd1i = 0;
	    wd3r = 1;
	    wd3i = 0;
	    k = 0;
	    for (j = 2; j < mh - 2; j += 4) {
	        k += 4;
	        wk1r = csc1 * (wd1r + w[w_start + k]);
	        wk1i = csc1 * (wd1i + w[w_start + k + 1]);
	        wk3r = csc3 * (wd3r + w[w_start + k + 2]);
	        wk3i = csc3 * (wd3i + w[w_start + k + 3]);
	        wd1r = w[w_start + k];
	        wd1i = w[w_start + k + 1];
	        wd3r = w[w_start + k + 2];
	        wd3i = w[w_start + k + 3];
	        j1 = j + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[j] + a[j2];
	        x0i = -a[j + 1] - a[j2 + 1];
	        x1r = a[j] - a[j2];
	        x1i = -a[j + 1] + a[j2 + 1];
	        y0r = a[j + 2] + a[j2 + 2];
	        y0i = -a[j + 3] - a[j2 + 3];
	        y1r = a[j + 2] - a[j2 + 2];
	        y1i = -a[j + 3] + a[j2 + 3];
	        x2r = a[j1] + a[j3];
	        x2i = a[j1 + 1] + a[j3 + 1];
	        x3r = a[j1] - a[j3];
	        x3i = a[j1 + 1] - a[j3 + 1];
	        y2r = a[j1 + 2] + a[j3 + 2];
	        y2i = a[j1 + 3] + a[j3 + 3];
	        y3r = a[j1 + 2] - a[j3 + 2];
	        y3i = a[j1 + 3] - a[j3 + 3];
	        a[j] = x0r + x2r;
	        a[j + 1] = x0i - x2i;
	        a[j + 2] = y0r + y2r;
	        a[j + 3] = y0i - y2i;
	        a[j1] = x0r - x2r;
	        a[j1 + 1] = x0i + x2i;
	        a[j1 + 2] = y0r - y2r;
	        a[j1 + 3] = y0i + y2i;
	        x0r = x1r + x3i;
	        x0i = x1i + x3r;
	        a[j2] = wk1r * x0r - wk1i * x0i;
	        a[j2 + 1] = wk1r * x0i + wk1i * x0r;
	        x0r = y1r + y3i;
	        x0i = y1i + y3r;
	        a[j2 + 2] = wd1r * x0r - wd1i * x0i;
	        a[j2 + 3] = wd1r * x0i + wd1i * x0r;
	        x0r = x1r - x3i;
	        x0i = x1i - x3r;
	        a[j3] = wk3r * x0r + wk3i * x0i;
	        a[j3 + 1] = wk3r * x0i - wk3i * x0r;
	        x0r = y1r - y3i;
	        x0i = y1i - y3r;
	        a[j3 + 2] = wd3r * x0r + wd3i * x0i;
	        a[j3 + 3] = wd3r * x0i - wd3i * x0r;
	        j0 = m - j;
	        j1 = j0 + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[j0] + a[j2];
	        x0i = -a[j0 + 1] - a[j2 + 1];
	        x1r = a[j0] - a[j2];
	        x1i = -a[j0 + 1] + a[j2 + 1];
	        y0r = a[j0 - 2] + a[j2 - 2];
	        y0i = -a[j0 - 1] - a[j2 - 1];
	        y1r = a[j0 - 2] - a[j2 - 2];
	        y1i = -a[j0 - 1] + a[j2 - 1];
	        x2r = a[j1] + a[j3];
	        x2i = a[j1 + 1] + a[j3 + 1];
	        x3r = a[j1] - a[j3];
	        x3i = a[j1 + 1] - a[j3 + 1];
	        y2r = a[j1 - 2] + a[j3 - 2];
	        y2i = a[j1 - 1] + a[j3 - 1];
	        y3r = a[j1 - 2] - a[j3 - 2];
	        y3i = a[j1 - 1] - a[j3 - 1];
	        a[j0] = x0r + x2r;
	        a[j0 + 1] = x0i - x2i;
	        a[j0 - 2] = y0r + y2r;
	        a[j0 - 1] = y0i - y2i;
	        a[j1] = x0r - x2r;
	        a[j1 + 1] = x0i + x2i;
	        a[j1 - 2] = y0r - y2r;
	        a[j1 - 1] = y0i + y2i;
	        x0r = x1r + x3i;
	        x0i = x1i + x3r;
	        a[j2] = wk1i * x0r - wk1r * x0i;
	        a[j2 + 1] = wk1i * x0i + wk1r * x0r;
	        x0r = y1r + y3i;
	        x0i = y1i + y3r;
	        a[j2 - 2] = wd1i * x0r - wd1r * x0i;
	        a[j2 - 1] = wd1i * x0i + wd1r * x0r;
	        x0r = x1r - x3i;
	        x0i = x1i - x3r;
	        a[j3] = wk3i * x0r + wk3r * x0i;
	        a[j3 + 1] = wk3i * x0i - wk3r * x0r;
	        x0r = y1r - y3i;
	        x0i = y1i - y3r;
	        a[j3 - 2] = wd3i * x0r + wd3r * x0i;
	        a[j3 - 1] = wd3i * x0i - wd3r * x0r;
	    }
	    wk1r = csc1 * (wd1r + wn4r);
	    wk1i = csc1 * (wd1i + wn4r);
	    wk3r = csc3 * (wd3r - wn4r);
	    wk3i = csc3 * (wd3i - wn4r);
	    j0 = mh;
	    j1 = j0 + m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[j0 - 2] + a[j2 - 2];
	    x0i = -a[j0 - 1] - a[j2 - 1];
	    x1r = a[j0 - 2] - a[j2 - 2];
	    x1i = -a[j0 - 1] + a[j2 - 1];
	    x2r = a[j1 - 2] + a[j3 - 2];
	    x2i = a[j1 - 1] + a[j3 - 1];
	    x3r = a[j1 - 2] - a[j3 - 2];
	    x3i = a[j1 - 1] - a[j3 - 1];
	    a[j0 - 2] = x0r + x2r;
	    a[j0 - 1] = x0i - x2i;
	    a[j1 - 2] = x0r - x2r;
	    a[j1 - 1] = x0i + x2i;
	    x0r = x1r + x3i;
	    x0i = x1i + x3r;
	    a[j2 - 2] = wk1r * x0r - wk1i * x0i;
	    a[j2 - 1] = wk1r * x0i + wk1i * x0r;
	    x0r = x1r - x3i;
	    x0i = x1i - x3r;
	    a[j3 - 2] = wk3r * x0r + wk3i * x0i;
	    a[j3 - 1] = wk3r * x0i - wk3i * x0r;
	    x0r = a[j0] + a[j2];
	    x0i = -a[j0 + 1] - a[j2 + 1];
	    x1r = a[j0] - a[j2];
	    x1i = -a[j0 + 1] + a[j2 + 1];
	    x2r = a[j1] + a[j3];
	    x2i = a[j1 + 1] + a[j3 + 1];
	    x3r = a[j1] - a[j3];
	    x3i = a[j1 + 1] - a[j3 + 1];
	    a[j0] = x0r + x2r;
	    a[j0 + 1] = x0i - x2i;
	    a[j1] = x0r - x2r;
	    a[j1 + 1] = x0i + x2i;
	    x0r = x1r + x3i;
	    x0i = x1i + x3r;
	    a[j2] = wn4r * (x0r - x0i);
	    a[j2 + 1] = wn4r * (x0i + x0r);
	    x0r = x1r - x3i;
	    x0i = x1i - x3r;
	    a[j3] = -wn4r * (x0r + x0i);
	    a[j3 + 1] = -wn4r * (x0i - x0r);
	    x0r = a[j0 + 2] + a[j2 + 2];
	    x0i = -a[j0 + 3] - a[j2 + 3];
	    x1r = a[j0 + 2] - a[j2 + 2];
	    x1i = -a[j0 + 3] + a[j2 + 3];
	    x2r = a[j1 + 2] + a[j3 + 2];
	    x2i = a[j1 + 3] + a[j3 + 3];
	    x3r = a[j1 + 2] - a[j3 + 2];
	    x3i = a[j1 + 3] - a[j3 + 3];
	    a[j0 + 2] = x0r + x2r;
	    a[j0 + 3] = x0i - x2i;
	    a[j1 + 2] = x0r - x2r;
	    a[j1 + 3] = x0i + x2i;
	    x0r = x1r + x3i;
	    x0i = x1i + x3r;
	    a[j2 + 2] = wk1i * x0r - wk1r * x0i;
	    a[j2 + 3] = wk1i * x0i + wk1r * x0r;
	    x0r = x1r - x3i;
	    x0i = x1i - x3r;
	    a[j3 + 2] = wk3i * x0r + wk3r * x0i;
	    a[j3 + 3] = wk3i * x0i - wk3r * x0r;
	}
	

	public static void cftrec4(int n, double []a, int nw, double []w)
	{
	
	    int isplt, j, k, m;
	    
	    m = n;
	    while (m > 512) {
	        m >>= 2;
	        cftmdl1(m, a, n - m, w, nw - (m >> 1));
	    }
	    cftleaf(m, 1, a, n - m, nw, w);
	    k = 0;
	    for (j = n - m; j > 0; j -= m) {
	        k++;
	        isplt = cfttree(m, j, k, a, nw, w);
	        cftleaf(m, isplt, a, j - m, nw, w);
	    }
	}


	public static int cfttree(int n, int j, int k, double []a, int nw, double []w)
	{
	    int i, isplt, m;
	    
	    if ((k & 3) != 0) {
	        isplt = k & 1;
	        if (isplt != 0) {
	            cftmdl1(n, a, j - n, w, nw - (n >> 1));
	        } else {
	            cftmdl2(n, a, j - n, w, nw - n);
	        }
	    } else {
	        m = n;
	        for (i = k; (i & 3) == 0; i >>= 2) {
	            m <<= 2;
	        }
	        isplt = i & 1;
	        if (isplt != 0) {
	            while (m > 128) {
	                cftmdl1(m, a, j - m, w, nw - (m >> 1));
	                m >>= 2;
	            }
	        } else {
	            while (m > 128) {
	                cftmdl2(m, a, j - m, w, nw - m);
	                m >>= 2;
	            }
	        }
	    }
	    return isplt;
	}


	public static void cftleaf(int n, int isplt, double []a, int a_start, int nw, double []w)
	{
	
	    
	    if (n == 512) {
	        cftmdl1(128, a, a_start + 0, w, nw - 64);
	        cftf161(a, a_start, w, nw - 8);
	        cftf162(a,a_start + 32, w,nw - 32);
	        cftf161(a,a_start + 64, w,nw - 8);
	        cftf161(a,a_start + 96, w,nw - 8);
	        cftmdl2(128, a,a_start + 128, w,nw - 128);
	        cftf161(a,a_start + 128, w,nw - 8);
	        cftf162(a,a_start + 160, w,nw - 32);
	        cftf161(a,a_start + 192, w,nw - 8);
	        cftf162(a,a_start + 224, w,nw - 32);
	        cftmdl1(128, a,a_start + 256, w,nw - 64);
	        cftf161(a,a_start + 256, w,nw - 8);
	        cftf162(a,a_start + 288, w,nw - 32);
	        cftf161(a,a_start + 320, w,nw - 8);
	        cftf161(a,a_start + 352, w,nw - 8);
	        if (isplt != 0) {
	            cftmdl1(128, a,a_start + 384, w,nw - 64);
	            cftf161(a,a_start + 480, w,nw - 8);
	        } else {
	            cftmdl2(128, a,a_start + 384, w, nw - 128);
	            cftf162(a, a_start + 480, w,nw - 32);
	        }
	        cftf161(a,a_start + 384, w,nw - 8);
	        cftf162(a,a_start + 416, w,nw - 32);
	        cftf161(a,a_start + 448, w,nw - 8);
	    } else {
	        cftmdl1(64, a, a_start, w,nw - 32);
	        cftf081(a, a_start, w,nw - 8);
	        cftf082(a,a_start + 16, w,nw - 8);
	        cftf081(a,a_start + 32, w,nw - 8);
	        cftf081(a,a_start + 48, w,nw - 8);
	        cftmdl2(64, a,a_start + 64, w,nw - 64);
	        cftf081(a,a_start + 64, w,nw - 8);
	        cftf082(a,a_start + 80, w,nw - 8);
	        cftf081(a,a_start + 96, w,nw - 8);
	        cftf082(a,a_start + 112, w,nw - 8);
	        cftmdl1(64, a,a_start + 128, w,nw - 32);
	        cftf081(a,a_start + 128, w,nw - 8);
	        cftf082(a,a_start + 144, w,nw - 8);
	        cftf081(a,a_start + 160, w,nw - 8);
	        cftf081(a,a_start + 176, w,nw - 8);
	        if (isplt != 0) {
	            cftmdl1(64, a,a_start + 192, w,nw - 32);
	            cftf081(a,a_start + 240, w,nw - 8);
	        } else {
	            cftmdl2(64, a,a_start + 192, w,nw - 64);
	            cftf082(a,a_start + 240, w,nw - 8);
	        }
	        cftf081(a,a_start + 192, w,nw - 8);
	        cftf082(a,a_start + 208, w,nw - 8);
	        cftf081(a,a_start + 224, w,nw - 8);
	    }
	}
	
	
	public static void cftmdl1(int n, double []a, int a_start, double []w, int w_start)
	{
	    int j, j0, j1, j2, j3, k, m, mh;
	    double wn4r, wk1r, wk1i, wk3r, wk3i;
	    double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
	    
	    mh = n >> 3;
	    m = 2 * mh;
	    j1 = m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[a_start + 0] + a[a_start + j2];
	    x0i = a[a_start + 1] + a[a_start + j2 + 1];
	    x1r = a[a_start + 0] - a[a_start + j2];
	    x1i = a[a_start + 1] - a[a_start + j2 + 1];
	    x2r = a[a_start + j1] + a[a_start + j3];
	    x2i = a[a_start + j1 + 1] + a[a_start + j3 + 1];
	    x3r = a[a_start + j1] - a[a_start + j3];
	    x3i = a[a_start + j1 + 1] - a[a_start + j3 + 1];
	    a[a_start + 0] = x0r + x2r;
	    a[a_start + 1] = x0i + x2i;
	    a[a_start + j1] = x0r - x2r;
	    a[a_start + j1 + 1] = x0i - x2i;
	    a[a_start + j2] = x1r - x3i;
	    a[a_start + j2 + 1] = x1i + x3r;
	    a[a_start + j3] = x1r + x3i;
	    a[a_start + j3 + 1] = x1i - x3r;
	    wn4r = w[w_start + 1];
	    k = 0;
	    for (j = 2; j < mh; j += 2) {
	        k += 4;
	        wk1r = w[w_start + k];
	        wk1i = w[w_start + k + 1];
	        wk3r = w[w_start + k + 2];
	        wk3i = w[w_start + k + 3];
	        j1 = j + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[a_start + j] + a[a_start + j2];
	        x0i = a[a_start + j + 1] + a[a_start + j2 + 1];
	        x1r = a[a_start + j] - a[a_start + j2];
	        x1i = a[a_start + j + 1] - a[a_start + j2 + 1];
	        x2r = a[a_start + j1] + a[a_start + j3];
	        x2i = a[a_start + j1 + 1] + a[a_start + j3 + 1];
	        x3r = a[a_start + j1] - a[a_start + j3];
	        x3i = a[a_start + j1 + 1] - a[a_start + j3 + 1];
	        a[a_start + j] = x0r + x2r;
	        a[a_start + j + 1] = x0i + x2i;
	        a[a_start + j1] = x0r - x2r;
	        a[a_start + j1 + 1] = x0i - x2i;
	        x0r = x1r - x3i;
	        x0i = x1i + x3r;
	        a[a_start + j2] = wk1r * x0r - wk1i * x0i;
	        a[a_start + j2 + 1] = wk1r * x0i + wk1i * x0r;
	        x0r = x1r + x3i;
	        x0i = x1i - x3r;
	        a[a_start + j3] = wk3r * x0r + wk3i * x0i;
	        a[a_start + j3 + 1] = wk3r * x0i - wk3i * x0r;
	        j0 = m - j;
	        j1 = j0 + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[a_start + j0] + a[a_start + j2];
	        x0i = a[a_start + j0 + 1] + a[a_start + j2 + 1];
	        x1r = a[a_start + j0] - a[a_start + j2];
	        x1i = a[a_start + j0 + 1] - a[a_start + j2 + 1];
	        x2r = a[a_start + j1] + a[a_start + j3];
	        x2i = a[a_start + j1 + 1] + a[a_start + j3 + 1];
	        x3r = a[a_start + j1] - a[a_start + j3];
	        x3i = a[a_start + j1 + 1] - a[a_start + j3 + 1];
	        a[a_start + j0] = x0r + x2r;
	        a[a_start + j0 + 1] = x0i + x2i;
	        a[a_start + j1] = x0r - x2r;
	        a[a_start + j1 + 1] = x0i - x2i;
	        x0r = x1r - x3i;
	        x0i = x1i + x3r;
	        a[a_start + j2] = wk1i * x0r - wk1r * x0i;
	        a[a_start + j2 + 1] = wk1i * x0i + wk1r * x0r;
	        x0r = x1r + x3i;
	        x0i = x1i - x3r;
	        a[a_start + j3] = wk3i * x0r + wk3r * x0i;
	        a[a_start + j3 + 1] = wk3i * x0i - wk3r * x0r;
	    }
	    j0 = mh;
	    j1 = j0 + m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[a_start + j0] + a[a_start + j2];
	    x0i = a[a_start + j0 + 1] + a[a_start + j2 + 1];
	    x1r = a[a_start + j0] - a[a_start + j2];
	    x1i = a[a_start + j0 + 1] - a[a_start + j2 + 1];
	    x2r = a[a_start + j1] + a[a_start + j3];
	    x2i = a[a_start + j1 + 1] + a[a_start + j3 + 1];
	    x3r = a[a_start + j1] - a[a_start + j3];
	    x3i = a[a_start + j1 + 1] - a[a_start + j3 + 1];
	    a[a_start + j0] = x0r + x2r;
	    a[a_start + j0 + 1] = x0i + x2i;
	    a[a_start + j1] = x0r - x2r;
	    a[a_start + j1 + 1] = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    a[a_start + j2] = wn4r * (x0r - x0i);
	    a[a_start + j2 + 1] = wn4r * (x0i + x0r);
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    a[a_start + j3] = -wn4r * (x0r + x0i);
	    a[a_start + j3 + 1] = -wn4r * (x0i - x0r);
	}


	public static void cftmdl2(int n, double []a, int a_start, double []w, int w_start)
	{
	    int j, j0, j1, j2, j3, k, kr, m, mh;
	    double wn4r, wk1r, wk1i, wk3r, wk3i, wd1r, wd1i, wd3r, wd3i;
	    double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, y0r, y0i, y2r, y2i;
	    
	    mh = n >> 3;
	    m = 2 * mh;
	    wn4r = w[1];
	    j1 = m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[a_start + 0] - a[a_start + j2 + 1];
	    x0i = a[a_start + 1] + a[a_start + j2];
	    x1r = a[a_start + 0] + a[a_start + j2 + 1];
	    x1i = a[a_start + 1] - a[a_start + j2];
	    x2r = a[a_start + j1] - a[a_start + j3 + 1];
	    x2i = a[a_start + j1 + 1] + a[a_start + j3];
	    x3r = a[a_start + j1] + a[a_start + j3 + 1];
	    x3i = a[a_start + j1 + 1] - a[a_start + j3];
	    y0r = wn4r * (x2r - x2i);
	    y0i = wn4r * (x2i + x2r);
	    a[a_start + 0] = x0r + y0r;
	    a[a_start + 1] = x0i + y0i;
	    a[a_start + j1] = x0r - y0r;
	    a[a_start + j1 + 1] = x0i - y0i;
	    y0r = wn4r * (x3r - x3i);
	    y0i = wn4r * (x3i + x3r);
	    a[a_start + j2] = x1r - y0i;
	    a[a_start + j2 + 1] = x1i + y0r;
	    a[a_start + j3] = x1r + y0i;
	    a[a_start + j3 + 1] = x1i - y0r;
	    k = 0;
	    kr = 2 * m;
	    for (j = 2; j < mh; j += 2) {
	        k += 4;
	        wk1r = w[k];
	        wk1i = w[k + 1];
	        wk3r = w[k + 2];
	        wk3i = w[k + 3];
	        kr -= 4;
	        wd1i = w[kr];
	        wd1r = w[kr + 1];
	        wd3i = w[kr + 2];
	        wd3r = w[kr + 3];
	        j1 = j + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[a_start + j] - a[a_start + j2 + 1];
	        x0i = a[a_start + j + 1] + a[a_start + j2];
	        x1r = a[a_start + j] + a[a_start + j2 + 1];
	        x1i = a[a_start + j + 1] - a[a_start + j2];
	        x2r = a[a_start + j1] - a[a_start + j3 + 1];
	        x2i = a[a_start + j1 + 1] + a[a_start + j3];
	        x3r = a[a_start + j1] + a[a_start + j3 + 1];
	        x3i = a[a_start + j1 + 1] - a[a_start + j3];
	        y0r = wk1r * x0r - wk1i * x0i;
	        y0i = wk1r * x0i + wk1i * x0r;
	        y2r = wd1r * x2r - wd1i * x2i;
	        y2i = wd1r * x2i + wd1i * x2r;
	        a[a_start + j] = y0r + y2r;
	        a[a_start + j + 1] = y0i + y2i;
	        a[a_start + j1] = y0r - y2r;
	        a[a_start + j1 + 1] = y0i - y2i;
	        y0r = wk3r * x1r + wk3i * x1i;
	        y0i = wk3r * x1i - wk3i * x1r;
	        y2r = wd3r * x3r + wd3i * x3i;
	        y2i = wd3r * x3i - wd3i * x3r;
	        a[a_start + j2] = y0r + y2r;
	        a[a_start + j2 + 1] = y0i + y2i;
	        a[a_start + j3] = y0r - y2r;
	        a[a_start + j3 + 1] = y0i - y2i;
	        j0 = m - j;
	        j1 = j0 + m;
	        j2 = j1 + m;
	        j3 = j2 + m;
	        x0r = a[a_start + j0] - a[a_start + j2 + 1];
	        x0i = a[a_start + j0 + 1] + a[a_start + j2];
	        x1r = a[a_start + j0] + a[a_start + j2 + 1];
	        x1i = a[a_start + j0 + 1] - a[a_start + j2];
	        x2r = a[a_start + j1] - a[a_start + j3 + 1];
	        x2i = a[a_start + j1 + 1] + a[a_start + j3];
	        x3r = a[a_start + j1] + a[a_start + j3 + 1];
	        x3i = a[a_start + j1 + 1] - a[a_start + j3];
	        y0r = wd1i * x0r - wd1r * x0i;
	        y0i = wd1i * x0i + wd1r * x0r;
	        y2r = wk1i * x2r - wk1r * x2i;
	        y2i = wk1i * x2i + wk1r * x2r;
	        a[a_start + j0] = y0r + y2r;
	        a[a_start + j0 + 1] = y0i + y2i;
	        a[a_start + j1] = y0r - y2r;
	        a[a_start + j1 + 1] = y0i - y2i;
	        y0r = wd3i * x1r + wd3r * x1i;
	        y0i = wd3i * x1i - wd3r * x1r;
	        y2r = wk3i * x3r + wk3r * x3i;
	        y2i = wk3i * x3i - wk3r * x3r;
	        a[a_start + j2] = y0r + y2r;
	        a[a_start + j2 + 1] = y0i + y2i;
	        a[a_start + j3] = y0r - y2r;
	        a[a_start + j3 + 1] = y0i - y2i;
	    }
	    wk1r = w[m];
	    wk1i = w[m + 1];
	    j0 = mh;
	    j1 = j0 + m;
	    j2 = j1 + m;
	    j3 = j2 + m;
	    x0r = a[a_start + j0] - a[a_start + j2 + 1];
	    x0i = a[a_start + j0 + 1] + a[a_start + j2];
	    x1r = a[a_start + j0] + a[a_start + j2 + 1];
	    x1i = a[a_start + j0 + 1] - a[a_start + j2];
	    x2r = a[a_start + j1] - a[a_start + j3 + 1];
	    x2i = a[a_start + j1 + 1] + a[a_start + j3];
	    x3r = a[a_start + j1] + a[a_start + j3 + 1];
	    x3i = a[a_start + j1 + 1] - a[a_start + j3];
	    y0r = wk1r * x0r - wk1i * x0i;
	    y0i = wk1r * x0i + wk1i * x0r;
	    y2r = wk1i * x2r - wk1r * x2i;
	    y2i = wk1i * x2i + wk1r * x2r;
	    a[a_start + j0] = y0r + y2r;
	    a[a_start + j0 + 1] = y0i + y2i;
	    a[a_start + j1] = y0r - y2r;
	    a[a_start + j1 + 1] = y0i - y2i;
	    y0r = wk1i * x1r - wk1r * x1i;
	    y0i = wk1i * x1i + wk1r * x1r;
	    y2r = wk1r * x3r - wk1i * x3i;
	    y2i = wk1r * x3i + wk1i * x3r;
	    a[a_start + j2] = y0r - y2r;
	    a[a_start + j2 + 1] = y0i - y2i;
	    a[a_start + j3] = y0r + y2r;
	    a[a_start + j3 + 1] = y0i + y2i;
	}


	public static void cftfx41(int n, double []a, int nw, double []w)
	{
	    
	    if (n == 128) {
	        cftf161(a, 0, w,nw - 8);
	        cftf162(a,32, w,nw - 32);
	        cftf161(a,64, w,nw - 8);
	        cftf161(a,96, w,nw - 8);
	    } else {
	        cftf081(a, 0, w,nw - 8);
	        cftf082(a,16, w,nw - 8);
	        cftf081(a,32, w,nw - 8);
	        cftf081(a,48, w,nw - 8);
	    }
	}


	public static void cftf161(double []a, int a_start, double []w, int w_start)
	{
	    double wn4r, wk1r, wk1i, 
	        x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, 
	        y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, 
	        y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i, 
	        y8r, y8i, y9r, y9i, y10r, y10i, y11r, y11i, 
	        y12r, y12i, y13r, y13i, y14r, y14i, y15r, y15i;
	    
	    wn4r = w[w_start + 1];
	    wk1r = w[w_start + 2];
	    wk1i = w[w_start + 3];
	    x0r = a[a_start + 0] + a[a_start + 16];
	    x0i = a[a_start + 1] + a[a_start + 17];
	    x1r = a[a_start + 0] - a[a_start + 16];
	    x1i = a[a_start + 1] - a[a_start + 17];
	    x2r = a[a_start + 8] + a[a_start + 24];
	    x2i = a[a_start + 9] + a[a_start + 25];
	    x3r = a[a_start + 8] - a[a_start + 24];
	    x3i = a[a_start + 9] - a[a_start + 25];
	    y0r = x0r + x2r;
	    y0i = x0i + x2i;
	    y4r = x0r - x2r;
	    y4i = x0i - x2i;
	    y8r = x1r - x3i;
	    y8i = x1i + x3r;
	    y12r = x1r + x3i;
	    y12i = x1i - x3r;
	    x0r = a[a_start + 2] + a[a_start + 18];
	    x0i = a[a_start + 3] + a[a_start + 19];
	    x1r = a[a_start + 2] - a[a_start + 18];
	    x1i = a[a_start + 3] - a[a_start + 19];
	    x2r = a[a_start + 10] + a[a_start + 26];
	    x2i = a[a_start + 11] + a[a_start + 27];
	    x3r = a[a_start + 10] - a[a_start + 26];
	    x3i = a[a_start + 11] - a[a_start + 27];
	    y1r = x0r + x2r;
	    y1i = x0i + x2i;
	    y5r = x0r - x2r;
	    y5i = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    y9r = wk1r * x0r - wk1i * x0i;
	    y9i = wk1r * x0i + wk1i * x0r;
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    y13r = wk1i * x0r - wk1r * x0i;
	    y13i = wk1i * x0i + wk1r * x0r;
	    x0r = a[a_start + 4] + a[a_start + 20];
	    x0i = a[a_start + 5] + a[a_start + 21];
	    x1r = a[a_start + 4] - a[a_start + 20];
	    x1i = a[a_start + 5] - a[a_start + 21];
	    x2r = a[a_start + 12] + a[a_start + 28];
	    x2i = a[a_start + 13] + a[a_start + 29];
	    x3r = a[a_start + 12] - a[a_start + 28];
	    x3i = a[a_start + 13] - a[a_start + 29];
	    y2r = x0r + x2r;
	    y2i = x0i + x2i;
	    y6r = x0r - x2r;
	    y6i = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    y10r = wn4r * (x0r - x0i);
	    y10i = wn4r * (x0i + x0r);
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    y14r = wn4r * (x0r + x0i);
	    y14i = wn4r * (x0i - x0r);
	    x0r = a[a_start + 6] + a[a_start + 22];
	    x0i = a[a_start + 7] + a[a_start + 23];
	    x1r = a[a_start + 6] - a[a_start + 22];
	    x1i = a[a_start + 7] - a[a_start + 23];
	    x2r = a[a_start + 14] + a[a_start + 30];
	    x2i = a[a_start + 15] + a[a_start + 31];
	    x3r = a[a_start + 14] - a[a_start + 30];
	    x3i = a[a_start + 15] - a[a_start + 31];
	    y3r = x0r + x2r;
	    y3i = x0i + x2i;
	    y7r = x0r - x2r;
	    y7i = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    y11r = wk1i * x0r - wk1r * x0i;
	    y11i = wk1i * x0i + wk1r * x0r;
	    x0r = x1r + x3i;
	    x0i = x1i - x3r;
	    y15r = wk1r * x0r - wk1i * x0i;
	    y15i = wk1r * x0i + wk1i * x0r;
	    x0r = y12r - y14r;
	    x0i = y12i - y14i;
	    x1r = y12r + y14r;
	    x1i = y12i + y14i;
	    x2r = y13r - y15r;
	    x2i = y13i - y15i;
	    x3r = y13r + y15r;
	    x3i = y13i + y15i;
	    a[a_start + 24] = x0r + x2r;
	    a[a_start + 25] = x0i + x2i;
	    a[a_start + 26] = x0r - x2r;
	    a[a_start + 27] = x0i - x2i;
	    a[a_start + 28] = x1r - x3i;
	    a[a_start + 29] = x1i + x3r;
	    a[a_start + 30] = x1r + x3i;
	    a[a_start + 31] = x1i - x3r;
	    x0r = y8r + y10r;
	    x0i = y8i + y10i;
	    x1r = y8r - y10r;
	    x1i = y8i - y10i;
	    x2r = y9r + y11r;
	    x2i = y9i + y11i;
	    x3r = y9r - y11r;
	    x3i = y9i - y11i;
	    a[a_start + 16] = x0r + x2r;
	    a[a_start + 17] = x0i + x2i;
	    a[a_start + 18] = x0r - x2r;
	    a[a_start + 19] = x0i - x2i;
	    a[a_start + 20] = x1r - x3i;
	    a[a_start + 21] = x1i + x3r;
	    a[a_start + 22] = x1r + x3i;
	    a[a_start + 23] = x1i - x3r;
	    x0r = y5r - y7i;
	    x0i = y5i + y7r;
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    x0r = y5r + y7i;
	    x0i = y5i - y7r;
	    x3r = wn4r * (x0r - x0i);
	    x3i = wn4r * (x0i + x0r);
	    x0r = y4r - y6i;
	    x0i = y4i + y6r;
	    x1r = y4r + y6i;
	    x1i = y4i - y6r;
	    a[a_start + 8] = x0r + x2r;
	    a[a_start + 9] = x0i + x2i;
	    a[a_start + 10] = x0r - x2r;
	    a[a_start + 11] = x0i - x2i;
	    a[a_start + 12] = x1r - x3i;
	    a[a_start + 13] = x1i + x3r;
	    a[a_start + 14] = x1r + x3i;
	    a[a_start + 15] = x1i - x3r;
	    x0r = y0r + y2r;
	    x0i = y0i + y2i;
	    x1r = y0r - y2r;
	    x1i = y0i - y2i;
	    x2r = y1r + y3r;
	    x2i = y1i + y3i;
	    x3r = y1r - y3r;
	    x3i = y1i - y3i;
	    a[a_start + 0] = x0r + x2r;
	    a[a_start + 1] = x0i + x2i;
	    a[a_start + 2] = x0r - x2r;
	    a[a_start + 3] = x0i - x2i;
	    a[a_start + 4] = x1r - x3i;
	    a[a_start + 5] = x1i + x3r;
	    a[a_start + 6] = x1r + x3i;
	    a[a_start + 7] = x1i - x3r;
	}


	public static void cftf162(double []a, int a_start, double []w, int w_start)
	{
	    double wn4r, wk1r, wk1i, wk2r, wk2i, wk3r, wk3i, 
	        x0r, x0i, x1r, x1i, x2r, x2i, 
	        y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, 
	        y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i, 
	        y8r, y8i, y9r, y9i, y10r, y10i, y11r, y11i, 
	        y12r, y12i, y13r, y13i, y14r, y14i, y15r, y15i;
	    
	    wn4r = w[w_start + 1];
	    wk1r = w[w_start + 4];
	    wk1i = w[w_start + 5];
	    wk3r = w[w_start + 6];
	    wk3i = -w[w_start + 7];
	    wk2r = w[w_start + 8];
	    wk2i = w[w_start + 9];
	    x1r = a[a_start + 0] - a[a_start + 17];
	    x1i = a[a_start + 1] + a[a_start + 16];
	    x0r = a[a_start + 8] - a[a_start + 25];
	    x0i = a[a_start + 9] + a[a_start + 24];
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    y0r = x1r + x2r;
	    y0i = x1i + x2i;
	    y4r = x1r - x2r;
	    y4i = x1i - x2i;
	    x1r = a[a_start + 0] + a[a_start + 17];
	    x1i = a[a_start + 1] - a[a_start + 16];
	    x0r = a[a_start + 8] + a[a_start + 25];
	    x0i = a[a_start + 9] - a[a_start + 24];
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    y8r = x1r - x2i;
	    y8i = x1i + x2r;
	    y12r = x1r + x2i;
	    y12i = x1i - x2r;
	    x0r = a[a_start + 2] - a[a_start + 19];
	    x0i = a[a_start + 3] + a[a_start + 18];
	    x1r = wk1r * x0r - wk1i * x0i;
	    x1i = wk1r * x0i + wk1i * x0r;
	    x0r = a[a_start + 10] - a[a_start + 27];
	    x0i = a[a_start + 11] + a[a_start + 26];
	    x2r = wk3i * x0r - wk3r * x0i;
	    x2i = wk3i * x0i + wk3r * x0r;
	    y1r = x1r + x2r;
	    y1i = x1i + x2i;
	    y5r = x1r - x2r;
	    y5i = x1i - x2i;
	    x0r = a[a_start + 2] + a[a_start + 19];
	    x0i = a[a_start + 3] - a[a_start + 18];
	    x1r = wk3r * x0r - wk3i * x0i;
	    x1i = wk3r * x0i + wk3i * x0r;
	    x0r = a[a_start + 10] + a[a_start + 27];
	    x0i = a[a_start + 11] - a[a_start + 26];
	    x2r = wk1r * x0r + wk1i * x0i;
	    x2i = wk1r * x0i - wk1i * x0r;
	    y9r = x1r - x2r;
	    y9i = x1i - x2i;
	    y13r = x1r + x2r;
	    y13i = x1i + x2i;
	    x0r = a[a_start + 4] - a[a_start + 21];
	    x0i = a[a_start + 5] + a[a_start + 20];
	    x1r = wk2r * x0r - wk2i * x0i;
	    x1i = wk2r * x0i + wk2i * x0r;
	    x0r = a[a_start + 12] - a[a_start + 29];
	    x0i = a[a_start + 13] + a[a_start + 28];
	    x2r = wk2i * x0r - wk2r * x0i;
	    x2i = wk2i * x0i + wk2r * x0r;
	    y2r = x1r + x2r;
	    y2i = x1i + x2i;
	    y6r = x1r - x2r;
	    y6i = x1i - x2i;
	    x0r = a[a_start + 4] + a[a_start + 21];
	    x0i = a[a_start + 5] - a[a_start + 20];
	    x1r = wk2i * x0r - wk2r * x0i;
	    x1i = wk2i * x0i + wk2r * x0r;
	    x0r = a[a_start + 12] + a[a_start + 29];
	    x0i = a[a_start + 13] - a[a_start + 28];
	    x2r = wk2r * x0r - wk2i * x0i;
	    x2i = wk2r * x0i + wk2i * x0r;
	    y10r = x1r - x2r;
	    y10i = x1i - x2i;
	    y14r = x1r + x2r;
	    y14i = x1i + x2i;
	    x0r = a[a_start + 6] - a[a_start + 23];
	    x0i = a[a_start + 7] + a[a_start + 22];
	    x1r = wk3r * x0r - wk3i * x0i;
	    x1i = wk3r * x0i + wk3i * x0r;
	    x0r = a[a_start + 14] - a[a_start + 31];
	    x0i = a[a_start + 15] + a[a_start + 30];
	    x2r = wk1i * x0r - wk1r * x0i;
	    x2i = wk1i * x0i + wk1r * x0r;
	    y3r = x1r + x2r;
	    y3i = x1i + x2i;
	    y7r = x1r - x2r;
	    y7i = x1i - x2i;
	    x0r = a[a_start + 6] + a[a_start + 23];
	    x0i = a[a_start + 7] - a[a_start + 22];
	    x1r = wk1i * x0r + wk1r * x0i;
	    x1i = wk1i * x0i - wk1r * x0r;
	    x0r = a[a_start + 14] + a[a_start + 31];
	    x0i = a[a_start + 15] - a[a_start + 30];
	    x2r = wk3i * x0r - wk3r * x0i;
	    x2i = wk3i * x0i + wk3r * x0r;
	    y11r = x1r + x2r;
	    y11i = x1i + x2i;
	    y15r = x1r - x2r;
	    y15i = x1i - x2i;
	    x1r = y0r + y2r;
	    x1i = y0i + y2i;
	    x2r = y1r + y3r;
	    x2i = y1i + y3i;
	    a[a_start + 0] = x1r + x2r;
	    a[a_start + 1] = x1i + x2i;
	    a[a_start + 2] = x1r - x2r;
	    a[a_start + 3] = x1i - x2i;
	    x1r = y0r - y2r;
	    x1i = y0i - y2i;
	    x2r = y1r - y3r;
	    x2i = y1i - y3i;
	    a[a_start + 4] = x1r - x2i;
	    a[a_start + 5] = x1i + x2r;
	    a[a_start + 6] = x1r + x2i;
	    a[a_start + 7] = x1i - x2r;
	    x1r = y4r - y6i;
	    x1i = y4i + y6r;
	    x0r = y5r - y7i;
	    x0i = y5i + y7r;
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    a[a_start + 8] = x1r + x2r;
	    a[a_start + 9] = x1i + x2i;
	    a[a_start + 10] = x1r - x2r;
	    a[a_start + 11] = x1i - x2i;
	    x1r = y4r + y6i;
	    x1i = y4i - y6r;
	    x0r = y5r + y7i;
	    x0i = y5i - y7r;
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    a[a_start + 12] = x1r - x2i;
	    a[a_start + 13] = x1i + x2r;
	    a[a_start + 14] = x1r + x2i;
	    a[a_start + 15] = x1i - x2r;
	    x1r = y8r + y10r;
	    x1i = y8i + y10i;
	    x2r = y9r - y11r;
	    x2i = y9i - y11i;
	    a[a_start + 16] = x1r + x2r;
	    a[a_start + 17] = x1i + x2i;
	    a[a_start + 18] = x1r - x2r;
	    a[a_start + 19] = x1i - x2i;
	    x1r = y8r - y10r;
	    x1i = y8i - y10i;
	    x2r = y9r + y11r;
	    x2i = y9i + y11i;
	    a[a_start + 20] = x1r - x2i;
	    a[a_start + 21] = x1i + x2r;
	    a[a_start + 22] = x1r + x2i;
	    a[a_start + 23] = x1i - x2r;
	    x1r = y12r - y14i;
	    x1i = y12i + y14r;
	    x0r = y13r + y15i;
	    x0i = y13i - y15r;
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    a[a_start + 24] = x1r + x2r;
	    a[a_start + 25] = x1i + x2i;
	    a[a_start + 26] = x1r - x2r;
	    a[a_start + 27] = x1i - x2i;
	    x1r = y12r + y14i;
	    x1i = y12i - y14r;
	    x0r = y13r - y15i;
	    x0i = y13i + y15r;
	    x2r = wn4r * (x0r - x0i);
	    x2i = wn4r * (x0i + x0r);
	    a[a_start + 28] = x1r - x2i;
	    a[a_start + 29] = x1i + x2r;
	    a[a_start + 30] = x1r + x2i;
	    a[a_start + 31] = x1i - x2r;
	}


	public static void cftf081(double []a, int a_start, double []w, int w_start)
	{
	    double wn4r, x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i, 
	        y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, 
	        y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i;
	    
	    wn4r = w[w_start + 1];
	    x0r = a[a_start + 0] + a[a_start + 8];
	    x0i = a[a_start + 1] + a[a_start + 9];
	    x1r = a[a_start + 0] - a[a_start + 8];
	    x1i = a[a_start + 1] - a[a_start + 9];
	    x2r = a[a_start + 4] + a[a_start + 12];
	    x2i = a[a_start + 5] + a[a_start + 13];
	    x3r = a[a_start + 4] - a[a_start + 12];
	    x3i = a[a_start + 5] - a[a_start + 13];
	    y0r = x0r + x2r;
	    y0i = x0i + x2i;
	    y2r = x0r - x2r;
	    y2i = x0i - x2i;
	    y1r = x1r - x3i;
	    y1i = x1i + x3r;
	    y3r = x1r + x3i;
	    y3i = x1i - x3r;
	    x0r = a[a_start + 2] + a[a_start + 10];
	    x0i = a[a_start + 3] + a[a_start + 11];
	    x1r = a[a_start + 2] - a[a_start + 10];
	    x1i = a[a_start + 3] - a[a_start + 11];
	    x2r = a[a_start + 6] + a[a_start + 14];
	    x2i = a[a_start + 7] + a[a_start + 15];
	    x3r = a[a_start + 6] - a[a_start + 14];
	    x3i = a[a_start + 7] - a[a_start + 15];
	    y4r = x0r + x2r;
	    y4i = x0i + x2i;
	    y6r = x0r - x2r;
	    y6i = x0i - x2i;
	    x0r = x1r - x3i;
	    x0i = x1i + x3r;
	    x2r = x1r + x3i;
	    x2i = x1i - x3r;
	    y5r = wn4r * (x0r - x0i);
	    y5i = wn4r * (x0r + x0i);
	    y7r = wn4r * (x2r - x2i);
	    y7i = wn4r * (x2r + x2i);
	    a[a_start + 8] = y1r + y5r;
	    a[a_start + 9] = y1i + y5i;
	    a[a_start + 10] = y1r - y5r;
	    a[a_start + 11] = y1i - y5i;
	    a[a_start + 12] = y3r - y7i;
	    a[a_start + 13] = y3i + y7r;
	    a[a_start + 14] = y3r + y7i;
	    a[a_start + 15] = y3i - y7r;
	    a[a_start + 0] = y0r + y4r;
	    a[a_start + 1] = y0i + y4i;
	    a[a_start + 2] = y0r - y4r;
	    a[a_start + 3] = y0i - y4i;
	    a[a_start + 4] = y2r - y6i;
	    a[a_start + 5] = y2i + y6r;
	    a[a_start + 6] = y2r + y6i;
	    a[a_start + 7] = y2i - y6r;
	}


	public static void cftf082(double []a, int a_start, double []w, int w_start)
	{
	    double wn4r, wk1r, wk1i, x0r, x0i, x1r, x1i, 
	        y0r, y0i, y1r, y1i, y2r, y2i, y3r, y3i, 
	        y4r, y4i, y5r, y5i, y6r, y6i, y7r, y7i;
	    
	    wn4r = w[w_start + 1];
	    wk1r = w[w_start + 2];
	    wk1i = w[w_start + 3];
	    y0r = a[a_start + 0] - a[a_start + 9];
	    y0i = a[a_start + 1] + a[a_start + 8];
	    y1r = a[a_start + 0] + a[a_start + 9];
	    y1i = a[a_start + 1] - a[a_start + 8];
	    x0r = a[a_start + 4] - a[a_start + 13];
	    x0i = a[a_start + 5] + a[a_start + 12];
	    y2r = wn4r * (x0r - x0i);
	    y2i = wn4r * (x0i + x0r);
	    x0r = a[a_start + 4] + a[a_start + 13];
	    x0i = a[a_start + 5] - a[a_start + 12];
	    y3r = wn4r * (x0r - x0i);
	    y3i = wn4r * (x0i + x0r);
	    x0r = a[a_start + 2] - a[a_start + 11];
	    x0i = a[a_start + 3] + a[a_start + 10];
	    y4r = wk1r * x0r - wk1i * x0i;
	    y4i = wk1r * x0i + wk1i * x0r;
	    x0r = a[a_start + 2] + a[a_start + 11];
	    x0i = a[a_start + 3] - a[a_start + 10];
	    y5r = wk1i * x0r - wk1r * x0i;
	    y5i = wk1i * x0i + wk1r * x0r;
	    x0r = a[a_start + 6] - a[a_start + 15];
	    x0i = a[a_start + 7] + a[a_start + 14];
	    y6r = wk1i * x0r - wk1r * x0i;
	    y6i = wk1i * x0i + wk1r * x0r;
	    x0r = a[a_start + 6] + a[a_start + 15];
	    x0i = a[a_start + 7] - a[a_start + 14];
	    y7r = wk1r * x0r - wk1i * x0i;
	    y7i = wk1r * x0i + wk1i * x0r;
	    x0r = y0r + y2r;
	    x0i = y0i + y2i;
	    x1r = y4r + y6r;
	    x1i = y4i + y6i;
	    a[a_start + 0] = x0r + x1r;
	    a[a_start + 1] = x0i + x1i;
	    a[a_start + 2] = x0r - x1r;
	    a[a_start + 3] = x0i - x1i;
	    x0r = y0r - y2r;
	    x0i = y0i - y2i;
	    x1r = y4r - y6r;
	    x1i = y4i - y6i;
	    a[a_start + 4] = x0r - x1i;
	    a[a_start + 5] = x0i + x1r;
	    a[a_start + 6] = x0r + x1i;
	    a[a_start + 7] = x0i - x1r;
	    x0r = y1r - y3i;
	    x0i = y1i + y3r;
	    x1r = y5r - y7r;
	    x1i = y5i - y7i;
	    a[a_start + 8] = x0r + x1r;
	    a[a_start + 9] = x0i + x1i;
	    a[a_start + 10] = x0r - x1r;
	    a[a_start + 11] = x0i - x1i;
	    x0r = y1r + y3i;
	    x0i = y1i - y3r;
	    x1r = y5r + y7r;
	    x1i = y5i + y7i;
	    a[a_start + 12] = x0r - x1i;
	    a[a_start + 13] = x0i + x1r;
	    a[a_start + 14] = x0r + x1i;
	    a[a_start + 15] = x0i - x1r;
	}


	public static void cftf040(double []a)
	{
	    double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
	    
	    x0r = a[0] + a[4];
	    x0i = a[1] + a[5];
	    x1r = a[0] - a[4];
	    x1i = a[1] - a[5];
	    x2r = a[2] + a[6];
	    x2i = a[3] + a[7];
	    x3r = a[2] - a[6];
	    x3i = a[3] - a[7];
	    a[0] = x0r + x2r;
	    a[1] = x0i + x2i;
	    a[2] = x1r - x3i;
	    a[3] = x1i + x3r;
	    a[4] = x0r - x2r;
	    a[5] = x0i - x2i;
	    a[6] = x1r + x3i;
	    a[7] = x1i - x3r;
	}


	public static void cftb040(double []a)
	{
	    double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
	    
	    x0r = a[0] + a[4];
	    x0i = a[1] + a[5];
	    x1r = a[0] - a[4];
	    x1i = a[1] - a[5];
	    x2r = a[2] + a[6];
	    x2i = a[3] + a[7];
	    x3r = a[2] - a[6];
	    x3i = a[3] - a[7];
	    a[0] = x0r + x2r;
	    a[1] = x0i + x2i;
	    a[2] = x1r + x3i;
	    a[3] = x1i - x3r;
	    a[4] = x0r - x2r;
	    a[5] = x0i - x2i;
	    a[6] = x1r - x3i;
	    a[7] = x1i + x3r;
	}


	public static void cftx020(double []a)
	{
	    double x0r, x0i;
	    
	    x0r = a[0] - a[2];
	    x0i = a[1] - a[3];
	    a[0] += a[2];
	    a[1] += a[3];
	    a[2] = x0r;
	    a[3] = x0i;
	}


	public static void rftfsub(int n, double []a, int nc, double []c)
	{
	    int j, k, kk, ks, m;
	    double wkr, wki, xr, xi, yr, yi;
	    
	    m = n >> 1;
	    ks = 2 * nc / m;
	    kk = 0;
	    for (j = 2; j < m; j += 2) {
	        k = n - j;
	        kk += ks;
	        wkr = 0.5 - c[nc - kk];
	        wki = c[kk];
	        xr = a[j] - a[k];
	        xi = a[j + 1] + a[k + 1];
	        yr = wkr * xr - wki * xi;
	        yi = wkr * xi + wki * xr;
	        a[j] -= yr;
	        a[j + 1] -= yi;
	        a[k] += yr;
	        a[k + 1] -= yi;
	    }
	}


	public static void rftbsub(int n, double []a, int nc, double []c)
	{
	    int j, k, kk, ks, m;
	    double wkr, wki, xr, xi, yr, yi;
	    
	    m = n >> 1;
	    ks = 2 * nc / m;
	    kk = 0;
	    for (j = 2; j < m; j += 2) {
	        k = n - j;
	        kk += ks;
	        wkr = 0.5 - c[nc - kk];
	        wki = c[kk];
	        xr = a[j] - a[k];
	        xi = a[j + 1] + a[k + 1];
	        yr = wkr * xr + wki * xi;
	        yi = wkr * xi - wki * xr;
	        a[j] -= yr;
	        a[j + 1] -= yi;
	        a[k] += yr;
	        a[k + 1] -= yi;
	    }
	}


	public static void dctsub(int n, double []a, int nc, double []c)
	{
	    int j, k, kk, ks, m;
	    double wkr, wki, xr;
	    
	    m = n >> 1;
	    ks = nc / n;
	    kk = 0;
	    for (j = 1; j < m; j++) {
	        k = n - j;
	        kk += ks;
	        wkr = c[kk] - c[nc - kk];
	        wki = c[kk] + c[nc - kk];
	        xr = wki * a[j] - wkr * a[k];
	        a[j] = wkr * a[j] + wki * a[k];
	        a[k] = xr;
	    }
	    a[m] *= c[0];
	}
	
	
	public static void dstsub(int n, double []a, int nc, double []c)
	{
	    int j, k, kk, ks, m;
	    double wkr, wki, xr;
	    
	    m = n >> 1;
	    ks = nc / n;
	    kk = 0;
	    for (j = 1; j < m; j++) {
	        k = n - j;
	        kk += ks;
	        wkr = c[kk] - c[nc - kk];
	        wki = c[kk] + c[nc - kk];
	        xr = wki * a[k] - wkr * a[j];
	        a[k] = wkr * a[k] + wki * a[j];
	        a[j] = xr;
	    }
	    a[m] *= c[0];
	}

}
