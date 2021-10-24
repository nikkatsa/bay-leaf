package com.nikoskatsanos.bayleaf.core.messagingpattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Request<REQUEST> {

    private String id;

    private REQUEST request;
}
