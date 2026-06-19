package wfcore.common.render;

import com.modularmods.mcgltf.animation.InterpolatedChannel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class AnimationLoop {

        public final List<InterpolatedChannel> animation;

        @Getter @Setter
        private boolean loop = false;

        @Getter
        private boolean finished = false;

        private float duration = -1f;


        public AnimationLoop(List<InterpolatedChannel> animation) {
            this.animation = animation;
        }

       public void update(float t) {
        float duration = getDuration();

        if (t >= duration) {
            if (loop) {
                t %= duration;
            } else {
                t = duration;
                finished = true;
            }
        }

        for (InterpolatedChannel channel : animation) {
            channel.update(t);
        }
    }


    public float getDuration() {
            if (duration < 0f) {
                float max = 0f;
                for (InterpolatedChannel c : animation) {
                    float[] keys = c.getKeys();
                    if (keys.length > 0) max = Math.max(max, keys[keys.length - 1]);
                }
                duration = max;
            }
            return duration;
        }
    }

