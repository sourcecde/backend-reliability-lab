create table orders (
                        id bigserial primary key,
                        uuid uuid not null unique,
                        product_id uuid not null,
                        quantity integer not null,
                        created_at timestamp not null default current_timestamp,
                        updated_at timestamp not null default current_timestamp
);