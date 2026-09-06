package com.eu.habbo.messages.outgoing.camera;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/**
 * Wire order follows Nitro's InitCameraMessageParser: creditPrice, ducketPrice,
 * publishDucketPrice. The two trailing ints (activity-point type charged for the
 * capture and for the publish) are an Octane extension the parser reads only when
 * they are present, so stock Nitro clients keep working.
 */
public class CameraPriceComposer extends MessageComposer {
    public final int credits;
    public final int points;
    public final int publishPoints;
    public final int pointsType;
    public final int publishPointsType;

    public CameraPriceComposer(int credits, int points, int publishPoints, int pointsType, int publishPointsType) {
        this.credits = credits;
        this.points = points;
        this.publishPoints = publishPoints;
        this.pointsType = pointsType;
        this.publishPointsType = publishPointsType;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CameraPriceComposer);
        this.response.appendInt(this.credits);
        this.response.appendInt(this.points);
        this.response.appendInt(this.publishPoints);
        this.response.appendInt(this.pointsType);
        this.response.appendInt(this.publishPointsType);
        return this.response;
    }

    public int getCredits() {
        return credits;
    }

    public int getPoints() {
        return points;
    }

    public int getPublishPoints() {
        return publishPoints;
    }

    public int getPointsType() {
        return pointsType;
    }

    public int getPublishPointsType() {
        return publishPointsType;
    }
}
