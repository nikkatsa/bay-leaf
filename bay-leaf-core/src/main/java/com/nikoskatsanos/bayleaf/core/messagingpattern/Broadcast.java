package com.nikoskatsanos.bayleaf.core.messagingpattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Broadcast<BROADCAST> {

    private BROADCAST broadcastMsg;
}
