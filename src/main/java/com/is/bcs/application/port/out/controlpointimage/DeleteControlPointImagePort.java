package com.is.bcs.application.port.out.controlpointimage;

public interface DeleteControlPointImagePort {

    void deleteByIdAndFlush(Long imageId);

}