// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.message;

public class Behavior {
    private String name;
    private Integer argument;

    public Behavior(String name, Integer argument) {
        this.name = name;
        this.argument = argument;
    }

    public String getName() {
        return name;
    }

    public Integer getArgument() {
        return argument;
    }

    @Override
    public String toString() {
        if (argument != null) {
            return name + ": " + argument;
        } else {
            return name;
        }
    }
}
