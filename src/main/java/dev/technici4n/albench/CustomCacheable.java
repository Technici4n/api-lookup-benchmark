package dev.technici4n.albench;

public interface CustomCacheable {
	void registerCallback(Runnable invalidateCallback);
}
