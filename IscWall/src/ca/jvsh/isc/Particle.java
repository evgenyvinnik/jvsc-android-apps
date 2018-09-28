package ca.jvsh.isc;

class Particle
{
	public boolean	isBeam		= false;
	public float	spx			= 0;
	public float	spy			= 0;

	public float	x;
	public float	y;

	public float	mn;
	public float	mvx;
	public float	mvy;

	public float	opacity		= 0;

	public int		mParticleType;

	// particles
	public int		mParticleSize;

	public Particle()
	{
		// mn = 0.1f + (float)Math.random()/8.0f;
		// mvx = 0.05f + (float)Math.random() / 4.0f;
		// mvy = 0.05f + (float)Math.random() / 8.0f;
		mn = 0.6f + IscWall.mRandom.nextFloat() / 2.0f;
		mvx = 0.4f + IscWall.mRandom.nextFloat();
		mvy = 0.4f + IscWall.mRandom.nextFloat() / 2.0f;

		mParticleType = IscWall.mRandom.nextInt(5);
		mParticleSize = IscWall.mRandom.nextInt(8);

		if (mParticleType == 2)
		{
			isBeam = true;
			mvy = 1.5f;
			mvx = mvy * 0.2758f;
			mParticleSize = IscWall.mRandom.nextInt(2);
		}
	}

}