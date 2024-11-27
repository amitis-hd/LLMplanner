(define (domain proj)

    (:action situps
            :parameters (?start - cell ?finish - cell)
            :precondition (and
                (blue_at ?start)
                ; move
                (and
                    (= (row ?start) (row ?finish))
                    (= (col ?start) (col ?finish))
                )
            )
            :effect (and
                (blue_at ?finish)
                (blue_at ?start)
            )
        )
)