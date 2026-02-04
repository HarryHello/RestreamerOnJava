package harryhelloo.restreamer.service;


import harryhelloo.restreamer.pojo.ObsWebsocket;

public interface ObsService {
    void startStream(ObsWebsocket obsWebsocket);

    void stopStream(ObsWebsocket obsWebsocket);

    void startRecord(ObsWebsocket obsWebsocket);

    void stopRecord(ObsWebsocket obsWebsocket);

    void setScene(ObsWebsocket obsWebsocket);
}
