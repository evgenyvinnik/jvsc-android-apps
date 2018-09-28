package ca.jvsh.audicy;

public class Tone
{
	////////////////////////////////////////////////////////////////////
	//tone variable
	////////////////////////////////////////////////////////////////////
	private float		numDecay;

	private float		phase;
	private float		phaseIncr;

	private int			bufferSize;

	public boolean end;

	public Tone(int fs, int bufferSize, int note)
	{
		this.bufferSize = bufferSize;
		end = false;

		numDecay = 20000;

		phase = 0.0f;
		phaseIncr = 110.0f * (float) Math.pow(2.0,  note /2.0f) / (float) (fs);
	}

	public void processAudio(float buffer[])
	{

		for (int i = 0; i < bufferSize; i++)
		{
			if (phase < 0.5f)
			{
				buffer[i] += (1.0f - (phase * 4.0f - 1.0f) * (phase * 4.0f - 1.0f)) * ( (float) numDecay / 20000.0f) * ( (float) numDecay / 20000.0f) * 0.0625f;
			}
			else
			{
				buffer[i] += ((phase * 4.0f - 3.0f) * (phase * 4.0f - 3.0f) - 1.0f) * ( (float) numDecay / 20000.0f) * ( (float) numDecay / 20000.0f) * 0.0625f;
			}

			phase += phaseIncr;

			if (phase >= 1)
				--phase;

			if (--numDecay <= 0)
			{
				end = true;
				return;
			}
		}

		end = false;
		return;
	}
}
