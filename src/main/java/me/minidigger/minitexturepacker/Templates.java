package me.minidigger.minitexturepacker;

public class Templates {

    public static final String PREDICATE = """
                { "predicate": {"custom_model_data":  %s}, "model": "%s"},
            """;

    public static final String BOW_PREDICATE = """
                { "predicate": { "custom_model_data": %s, "pulling": %s, "pull": %s}, "model": "%s"},
            """;

    //language=JSON
    public static final String BOW = """
            {
              "parent": "item/generated",
              "textures": {
                "layer0": "item/bow"
              },
              "display": {
                "thirdperson_righthand": {
                  "rotation": [ -80, 260, -40 ],
                  "translation": [ -1, -2, 2.5 ],
                  "scale": [ 0.9, 0.9, 0.9 ]
                },
                "thirdperson_lefthand": {
                  "rotation": [ -80, -280, 40 ],
                  "translation": [ -1, -2, 2.5 ],
                  "scale": [ 0.9, 0.9, 0.9 ]
                },
                "firstperson_righthand": {
                  "rotation": [ 0, -90, 25 ],
                  "translation": [ 1.13, 3.2, 1.13],
                  "scale": [ 0.68, 0.68, 0.68 ]
                },
                "firstperson_lefthand": {
                  "rotation": [ 0, 90, -25 ],
                  "translation": [ 1.13, 3.2, 1.13],
                  "scale": [ 0.68, 0.68, 0.68 ]
                }
              },
              "overrides": [
                {
                  "predicate": {
                    "pulling": 1
                  },
                  "model": "item/bow_pulling_0"
                },
                {
                  "predicate": {
                    "pulling": 1,
                    "pull": 0.65
                  },
                  "model": "item/bow_pulling_1"
                },
                {
                  "predicate": {
                    "pulling": 1,
                    "pull": 0.9
                  },
                  "model": "item/bow_pulling_2"
                },
                %mini_model_creator_marker%
              ]
            }
            """;
}
