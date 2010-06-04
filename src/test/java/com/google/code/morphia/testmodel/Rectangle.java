/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia.testmodel;

import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.testutil.TestEntity;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Rectangle extends TestEntity implements Shape {
	private static final long serialVersionUID = 1L;
	
	@Property
    private double height;
    @Property
    private double width;

    public Rectangle() {
        super();
    }

    public Rectangle(double height, double width) {
        super();
        this.height = height;
        this.width = width;
    }

    public double getArea() {
        return height * width;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }
}
