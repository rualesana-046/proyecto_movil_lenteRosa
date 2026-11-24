package com.lenterosa.api.places.application;

import com.lenterosa.api.places.ReviewContributed;

/** Puerto para avisar a otros módulos que se publicó una opinión. La infraestructura lo conecta a Spring. */
@FunctionalInterface
public interface ContributionEvents {

    void published(ReviewContributed event);
}
